package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.network.commands.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.util.concurrent.*

/**
 * Представляет точку обмена трафиком пользовательского канала
 */
class NetworkSenderSocket(private val selfId: HostID, controlBus: IChannel, cancellationToken: CancellationToken, logger: Logger?) :
        SenderSocket(cancellationToken, logger) {

    /**
     * Тип передаваемого пакета
     */
    enum class PacketID(val id: Int) {

        Datagram(1),
        Stream(2),
        Ping(3),
        PingReply(4),
    }

    private val udp: SafeUdpClient
    private val fmt = Formatter()
    private val _endpoints = ConcurrentHashMap<InetSocketAddress, Deferred<NetworkEndPoint?>>()
    private val _epmap = ConcurrentHashMap<HostID, NetworkEndPoint>()

    val basePort: Int
        get() = udp.localEndPoint.port

    // val tcp: ServerSocket

    init {
        udp = SafeUdpClient(InetSocketAddress(InetAddress.getByName("::"), 0), cancellationToken, false, received = ::udpReceived)

        logger?.writeLog(LogImportance.Info, logCat, "Открыт UDP сокет по адресу ${udp.localEndPoint}")

        controlBus.getChannel(::onCmd)

        //tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("::", 0))
        //launch(Dispatchers.IO) { beginAccept() } <- for stream over tcp
    }

    private fun onCmd(@Suppress("UNUSED_PARAMETER") ch: IChannelEndPoint, msg: Message) {
        when (msg) {
            is RebindCommand -> {
                udp.bind()
                logger?.writeLog(LogImportance.Info, logCat, "Rebind $udp")
            }
        }
    }

    @Suppress("unused", "RedundantSuspendModifier")
    private suspend fun beginAccept() {

/*        val s = tcp.accept()

        beginAccept()

        val rc = s.openReadChannel()

        try {
            val t = AckToken(BinaryReader(rc))

            t.received(rc)
        } catch (e: Exception) {
            return
        }*/
    }

    /**
     * Обработка принятого из сети пакета данных
     *
     * @param bytes Принятый пакет данных
     * @param from Отправитель пакета данных
     */
    private fun udpReceived(bytes: ByteArray, from: InetSocketAddress) {
        launch {
            val rd = BinaryReader(bytes)

            val b = rd.readByte()

            val pid = PacketID.values().find { it.id == b.toInt() }

            when (pid) {
                PacketID.Datagram -> receiveDatagram(rd, from)
                PacketID.Ping -> ping(rd, from)
                PacketID.PingReply -> pingReply(rd, from)
                else -> {
                }
            }
        }
    }

    /**
     * Обработка Ping пакета
     *
     * Отправляет в ответ пакет PingReply с меткой, переданной в Ping пакете и со своим HostID
     */
    private fun ping(reader: BinaryReader, from: InetSocketAddress) {
        val tk = AckToken(reader)
        val wr = BinaryWriter()

        logger?.writeLog(LogImportance.Trace, logCat, "Ping from $from, token $tk")

        wr.write(PacketID.PingReply.id.toByte())
        wr.write(tk)
        wr.write(selfId)

        udp.send(wr.toArray(), from)
    }

    /**
     * Регистрирует конечную точку
     */
    fun register(networkEndPoint: NetworkEndPoint) {
        _epmap[networkEndPoint.targetHost] = networkEndPoint
    }

    /**
     * Разрегистрирует конечную точку
     */
    fun unregister(networkEndPoint: NetworkEndPoint) {
        _epmap.remove(networkEndPoint.targetHost)
    }

    /**
     * Обработка PingReply пакета
     *
     * Уведомляет асинхронно ожидающих его прихода
     */
    private fun pingReply(reader: BinaryReader, @Suppress("UNUSED_PARAMETER") from: InetSocketAddress) = AckToken(reader).received(reader)

    /**
     * Асинхронно разрешает сетевой адрес в объект конечной точки канала
     *
     * Разрешение происходит путем отправки Ping пакета на сетевой адрес, и приема ответа
     * с содержащимся в нем идентификатором хоста.
     * Производится до 3х попыток c ограничением времени ожидания в 500мс каждая.
     *
     * При неудаче возвращается *null*.
     *
     * @param IPEndPoint Проверяемый сетевой адрес
     *
     * @return Конечная точка канала
     */
    private suspend fun resolveIPEndPoint(IPEndPoint: InetSocketAddress): NetworkEndPoint? {
        logger?.writeLog(LogImportance.Info, logCat, "Разрешение неизвестного источника $IPEndPoint")

        // Три попытки разрешения
        for (i in 1..3) {
            val wr = BinaryWriter()

            val (tk, j) = register<BinaryReader>(CancellationTokenSource(500).token)

            wr.write((PacketID.Ping.id.toByte()))
            wr.write(tk)

            udp.send(wr.toArray(), IPEndPoint)

            val rd = j.cancelSafeAwait() ?: continue
            val ep = _epmap[HostID(rd)] ?: continue

            logger?.writeLog(LogImportance.Info, logCat, "Источник $IPEndPoint успешно разрешен с $i попытки. Цель $ep")

            return ep
        }

        logger?.writeLog(LogImportance.Warning, logCat, "Источник $IPEndPoint разрешить не удалось")

        return null
    }

    /**
     * Асинхронно разрешает сетевой адрес в объект конечной точки канала
     *
     * Разрешение происходит путем отправки Ping пакета на сетевой адрес, и приема ответа
     * с содержащимся в нем идентификатором хоста.
     * Производится до 3х попыток c ограничением времени ожидания в 500мс каждая.
     *
     * Результат неудачного разрешения адреса кешируется на 2 сек, а затем удаляется.
     *
     * @return Объект ожидания завершения операции разрешения адреса
     */
    @Suppress("DeferredIsResult")
    private fun resolve(remote: InetSocketAddress): Deferred<NetworkEndPoint?> {

        return _endpoints.getOrPut(remote) {
            val t = async { resolveIPEndPoint(remote) }

            launch {
                if (t.await() != null)
                    return@launch

                delay(2000)

                @Suppress("DeferredResultUnused")
                _endpoints.remove(remote)
            }

            return@getOrPut t
        }
    }

    /**
     * Разбор принятой из сети датаграммы.
     * Чтение метаданных, тела сообщений и передача сформрованного пакета конечной точке на обработку.
     */
    private suspend fun receiveDatagram(reader: BinaryReader, from: InetSocketAddress) {

        val ep = resolve(from).await() ?: return

        @Suppress("UNCHECKED_CAST")
        val t = fmt.deserialize(reader) as? Array<String>?

        fmt.deserialize(reader) // metadata
        val d = fmt.deserialize(reader) as ByteArray

        ep.onReceived(BytesPacket(d, ep.targetHost, HostID.Local).apply { tags = t })
    }

    /**
     * Передача датаграммы удаленному хосту
     * @param data Передаваемые данные
     * @param Target Адрес точки приема удаленного хоста
     */
    override fun sendDatagram(data: BytesPacket, Target: EndPoint) {

        if (Target !is NetworkEndPoint)
            return

        val d = data.data
        val tgt = Target.target ?: return

        val wr = BinaryWriter()

        wr.write(PacketID.Datagram.id.toByte()) // Маркер байтовой посылки

        fmt.serialize(wr, data.tags)
        fmt.serialize(wr, data.metadata) // Метаданные
        fmt.serialize(wr, d)             // Данные

        udp.send(wr.toArray(), tgt)
    }

    override fun safeClose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
