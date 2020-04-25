package pvt.psk.jcore.relay

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.io.*

/**
 * Ретранслятор упакованных в байтовые посылки сообщений
 *
 * @param T Тип данных, представляющих адрес отправителя пакета
 */
abstract class PacketBasedRelay<T>(relayID: RelayID) : BaseRelay(relayID) {

    private enum class PacketType(val id: Int) {
        Reply(1),
        RelayInfo(2),
        Serialized(3),
        Datagram(4),
        DatagramOverStream(5),
        Stream(6);

        companion object {
            fun fromInt(value: Int) = PacketType.values().first { it.id == value }
        }
    }

    private val _formatter = Formatter()

    /**
     * Байтовые посылки, размер которых выше этого порога, будут переданы отдельным потоком
     */
    val DatagramOverStreamThreshold = 1250;

    /**
     * Упаковка сообщений в поток байтов
     *
     * @param Message Упаковываемое сообщение
     * @param writer Поток, в который производится упаковка
     *
     * @return Признак успешно проведенной упаковки
     */
    protected fun serialize(Message: RelayMessage, writer: BinaryWriter): Boolean {

        fun writePreamble() {
            writer.write(Message.source);         // Ретранслятор отправитель
            writer.write(Message.targetRelay);    // Ретранслятор получатель
            writer.write(Message.ttl.toByte());   // Время жизни пакета

            val rt = Message.relaysTokens;        // Список токенов ретрансляторов, через которые пакет уже прошел
            writer.write(rt.size.toByte());

            rt.forEach(writer::write)
        }

        fun writeEnvelope() {
            if (Message.payload !is RelayEnvelope)
                return

            val rre = Message.payload

            writer.write(rre.from)
            writer.write(rre.targets)
        }

        val msg = if (Message.payload is RelayEnvelope)
            Message.payload.payload else Message.payload

        // Метки
        // Метаданные
        when (msg) {
            is RelayInfo     -> {
                writer.write(PacketType.RelayInfo.id.toByte())
                writePreamble()
                msg.serialize(writer)
            }

            is ISerializable -> {
                writer.write(PacketType.Serialized.id.toByte())
                writePreamble()
                writeEnvelope()
                if (!writeSerializable(msg, writer))
                    return false
            }

            is BytesPacket   -> {
                sendDatagram(msg, writer) {
                    writePreamble()
                    writeEnvelope()
                }
            }

            is StreamPacket  -> {
                writer.write(PacketType.Stream.id.toByte())
                writePreamble()
                writeEnvelope()
                msg.tag.serialize(writer)
                _formatter.serialize(writer, msg.metadata)

                sendStream(msg)(writer)
            }
        }

        return true
    }

    fun serializeReply(token: AckToken, writer: BinaryWriter) {
        writer.write(PacketType.Reply.id.toByte())
        writer.write(token)
    }

    /**
     * Асинхронная распаковка сообщения из потока
     *
     * @param reader Исходный поток, содержащий сообщение
     * @param From Низкоуровневый адрес ретранслятора, отправившего сообщение
     *
     * @return Распакованное сообщение
     */
    protected suspend fun deserialize(reader: BinaryReader, From: T): RelayMessage? {

        val type = PacketType.fromInt(reader.readByte())

        if (type == PacketType.Reply) {
            AckToken(reader).received(From to reader)
            return null
        }

        val srcRelay = RelayID(reader); // Идентификатор ретранслятора, приславшего пакет
        val tgtRelay = RelayID(reader); // Идентификатор адресата сообщения

        if (srcRelay == relayID || tgtRelay != relayID)
            return null

        val ttl = reader.readByte().toUInt()

        val tc = Array(reader.readByte()) { reader.readInt32() }.toIntArray()

        val rm: Any? = when (type) {
            PacketType.RelayInfo          -> readRelayInfo(reader)
            PacketType.Datagram           -> readBytesPacket(reader)
            PacketType.Stream             -> readStreamPacket(reader, From)
            PacketType.DatagramOverStream -> readDatagramOverStreamPacket(reader, From)
            PacketType.Serialized         -> readSerializable(reader)
            else                          -> null
        }

        if (rm == null)
            return null

        return RelayMessage(srcRelay, tgtRelay, rm, ttl, tc)
    }

    /**
     * Чтение информации о ретрансляторе
     */
    private fun readRelayInfo(reader: BinaryReader) = RelayInfo(reader)

    /**
     * Чтение информации об отправителе/получателе пакета
     */
    private fun readEnvelope(reader: BinaryReader): Pair<HostEndpointID, HostEndpointIDSet> {
        val from = HostEndpointID(reader)
        val l = HostEndpointIDSet(reader)

        return Pair(from, l)
    }

    /**
     * Прием байтовой посылки
     *
     * @param Reader Сериализованная посылка
     *
     * @return Пакет байтовой посылки
     */
    private fun readBytesPacket(Reader: BinaryReader): RelayEnvelope {
        val (s, t) = readEnvelope(Reader)

        val tags = PacketTag(Reader)
        val mtd = _formatter.deserialize(Reader) as? Array<Any>?
        val data = _formatter.deserialize(Reader) as ByteArray

        return RelayEnvelope(s, t, BytesPacket(data, mtd).apply { tag = tags })
    }

    /**
     * Прием посылки, передаваемой отдельным потоком
     */
    private suspend fun readStreamPacket(Reader: BinaryReader, From: T): RelayEnvelope? {
        val (s, t) = readEnvelope(Reader)

        val sp = createStreamPacketAsync(Reader, _formatter, From) ?: return null

        return RelayEnvelope(s, t, sp)
    }

    /**
     * Прием большого байтового массива, переданного отдельным потоком
     */
    private suspend fun readDatagramOverStreamPacket(Reader: BinaryReader, From: T): RelayEnvelope? {
        val (s, t) = readEnvelope(Reader)

        val expected = Reader.readInt32()

        val osp = createStreamPacketAsync(Reader, _formatter, From) ?: return null

        val ms = ByteArrayOutputStream()
        osp.addTargetStream(ms)

        osp.completeStreamAsync()

        osp.dispose()

        val bytes = ms.toByteArray()

        if (bytes.size != expected)    // Проверка что весь пакет принят полностью
            return null

        return RelayEnvelope(s, t, BytesPacket(bytes, osp.metadata))
    }

    /**
     * Отправка пакета данных
     */
    private fun sendDatagram(data: BytesPacket, writer: BinaryWriter, writePreamble: () -> Unit) {

        if (data.data.size <= DatagramOverStreamThreshold) {
            writer.write(PacketType.Datagram.id.toByte())
            writePreamble()

            data.tag.serialize(writer)
            _formatter.serialize(writer, data.metadata)
            _formatter.serialize(writer, data.data)
        } else {
            writer.write(PacketType.DatagramOverStream.id.toByte())
            writePreamble()

            writer.write(data.data.size)
            data.tag.serialize(writer)
            _formatter.serialize(writer, data.metadata)

            val sp = StreamPacket().apply { sourceStream = ByteArrayInputStream(data.data) }
            sendStream(sp)(writer)

            launch {
                sp.completeStreamAsync()
                sp.dispose()
            }
        }
    }

    /**
     * Упаковка объекта, реализующего интерфейс ISerializable
     */
    private fun writeSerializable(value: ISerializable, writer: BinaryWriter): Boolean {
        val t = when (value) {
            is HostInfoCommand -> 1
            else               -> 0
        }

        if (t == 0)
            return false

        writer.write(t.toByte())
        writer.write(value)

        return true
    }

    fun readSerializable(reader: BinaryReader): Any? {
        val (s, t) = readEnvelope(reader)

        return when (reader.readByte()) {
            1    -> HostInfoCommand(reader)
            else -> null
        }?.let { RelayEnvelope(s, t, it) }
    }

    /**
     * Отправка отдельного потока
     */
    abstract fun sendStream(msg: StreamPacket): (BinaryWriter) -> Unit

    /**
     * Прием отдельного потока
     */
    protected abstract suspend fun createStreamPacketAsync(reader: BinaryReader, formatter: Formatter, from: T): StreamPacket?
}