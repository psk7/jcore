package pvt.psk.jcore.network

import kotlinx.coroutines.*
import org.koin.core.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*
import java.net.*

enum class AddressFamily {
    IPv4,
    IPv6
}

abstract class BaseNetworkRelay(relayID: RelayID) : PacketBasedRelay<InetSocketAddress>(relayID) {

    interface UdpClientFactory {
        fun create(bindEndPoint: InetSocketAddress, received: (ByteArray, InetSocketAddress) -> Unit): SafeUdpClient
    }

    protected val udpFactory: UdpClientFactory by inject()

    /**
     * Список граничащих ретрансляторов с их сетевыми адресами
     */
    protected val relays = ConcurrentPriorityHashMap<RelayID, InetSocketAddress>()

    protected val remotePeers = ConcurrentHashSet<InetSocketAddress>()

    val admPort: Int
        get() = iAdmPort

    protected var iAdmPort: Int = 0

    private val debugHolder: DebugInfoHolder by inject()

    /**
     * Обработка принятого из сети пакета
     *
     * @param data Пакет данных
     * @param from Отправитель пакета данных
     */
    protected suspend fun received(data: ByteArray, from: InetSocketAddress) {

        if (data.size < 6)
            return

        if (data[0] == 0xaa.toByte()) {
            when {
                data[1] == 1.toByte() -> processHello(data, from)
                data[1] == 2.toByte() -> processBye(data)
            }
        } else {
            val rm = deserialize(BinaryReader(data), from) ?: return

            received(rm)

            if (!(rm.payload is RelayEnvelope && rm.payload.payload is StreamPacket))
                return

            val sp: StreamPacket = rm.payload.payload
            sp.completeStreamAsync()

            //println("----------------- Transferred ${(sp.stream as ForkStream).written} bytes")
            //println("----------------- BAOS size ${baos.toByteArray().size} bytes")

            sp.dispose()
        }
    }

    private fun processHello(data: ByteArray, from: InetSocketAddress) {
        val rd = BinaryReader(data)
        rd.readByte()   // Пропускаем 1 идентификатор пакета
        rd.readByte()   // Пропускаем 2 идентификатор пакета

        val relayID = RelayID(rd)

        if (relayID == this.relayID)
            return // Получен обратно свой пакет

        val known = Array(rd.readByte()) { RelayID(rd) }

        if (relays.addOrUpdate(relayID, scoreIPAddress(from.address), from)) {
            logger.writeLog(LogImportance.Info, logCat, "Обнаружен ретранслятор: $relayID@$from}")

            addAdjacentRelay(relayID, ::send)

            sendHello(arrayOf(from))
        } else {
            if (known.any { it == this.relayID })
                return

            // Если отправитель не знает этот ретранслятор - ему отправляется Hello в ответ
            sendHello(arrayOf(from))
        }

        debugHolder.remotes = relays.values.toTypedArray()
    }

    fun processBye(data: ByteArray) {
        val rd = BinaryReader(data)
        rd.readByte()   // Пропускаем 1 идентификатор пакета
        rd.readByte()   // Пропускаем 2 идентификатор пакета

        val relayID = RelayID(rd)

        if (relayID == this.relayID)
            return // Получен обратно свой пакет

        removeAdjacentRelay(relayID)

        relays.remove(relayID)
    }

    /**
     * Отправка сообщения в сеть
     *
     * @param message Отправляемое сообщение
     */
    protected fun send(message: RelayMessage): Boolean {
        if (message.source == message.targetRelay) // Обратно пакет не отправляется
            return false

        if (message.payload is RelayEnvelope && message.payload.targets.isEmpty())
            return false

        val ep = relays.tryGetValue(message.targetRelay) ?: return false

        val wr = BinaryWriter()

        if (!serialize(message, wr))
            return false

        sendDatagramRepeatedly(message, wr.toArray(), ep)
        return true
    }

    /**
     * Перегрузка этого метода может отправлять сообщение многократно при необходимости
     *
     * @param message Исходное сообщение
     * @param serializedMessage Сериализованное сообщение
     * @param to Получатель сообщения
     */
    protected open fun sendDatagramRepeatedly(message: RelayMessage, serializedMessage: ByteArray,
                                              to: InetSocketAddress) {
        sendDatagram(serializedMessage, to)
    }

    /**
     * Отправка байтовой посылки в сеть
     *
     * @param data Сообщение
     * @param target IP адрес ретранслятора цели
     */
    protected abstract fun sendDatagram(data: ByteArray, target: InetSocketAddress)

    protected fun sendStreamPassive(packet: StreamPacket, callbackPort: Int): (BinaryWriter) -> Unit {
        val tk = registerAckToken()

        packet.addTargetStream(async { tk.await<Pair<NetworkInputStream, NetworkOutputStream>>()?.second })

        return { wr ->
            wr.write(true)
            wr.write(callbackPort.toUShort())
            wr.write(tk)
        }
    }

    protected fun sendStreamActive(packet: StreamPacket): (BinaryWriter) -> Unit {
        val token = registerAckToken()

        val stream = async {
            val (from, rd) = token.await<Pair<InetSocketAddress, BinaryReader>>() ?: return@async null

            val tkr = AckToken(rd)
            val port = rd.readUInt16()

            connectTcpToWrite(from.address, port.toInt(), tkr)
        }

        packet.addTargetStream(stream)

        return { wr ->
            wr.write(false)
            wr.write(token)
        }
    }

    override suspend fun createStreamPacketAsync(reader: BinaryReader,
                                                 formatter: Formatter,
                                                 from: InetSocketAddress): StreamPacket? {

        val tag = PacketTag(reader)
        val mtd = formatter.deserialize(reader) as Array<Any>?

        val rm = when (reader.readByte() != 0) {
            true  -> {
                val callbackPort = reader.readUInt16()
                val token = AckToken(reader)

                receiveStreamActive(mtd, callbackPort.toInt(), token, from)
            }
            false -> receiveStreamPassive(mtd, AckToken(reader), from)
        }

        if (rm != null)
            rm.tag = tag

        return rm
    }

    /**
     * Прием отдельного потока из сети
     */
    protected suspend fun receiveStreamActive(metadata: Array<Any>?,
                                              port: Int,
                                              token: AckToken,
                                              from: InetSocketAddress): StreamPacket? {
        val ns = connectTcpToRead(from.address, port, token) ?: return null

        return StreamPacket(metadata).apply { sourceStream = ns }
    }

    protected suspend fun receiveStreamPassive(metadata: Array<Any>?,
                                               token: AckToken,
                                               from: InetSocketAddress): StreamPacket? {
        val wr = BinaryWriter()

        serializeReply(token, wr)

        val ntk = registerAckToken()

        wr.write(ntk)

        serializeTcpListenerPort(wr)

        sendDatagram(wr.toArray(), from)

        return StreamPacket(metadata).apply {
            sourceStream = ntk.await<Pair<NetworkInputStream, NetworkOutputStream>>()?.first ?: return null
        }
    }

    protected open fun serializeTcpListenerPort(writer: BinaryWriter) {}

    open val tcpConnectionTimeout = 0
    open val tcpReadTimeout = 0

    private suspend fun connectTcp(address: InetAddress, port: Int): Socket? = withContext(Dispatchers.IO) {
        try {
            val s = Socket()
            s.connect(InetSocketAddress(address, port), tcpConnectionTimeout)
            s.soTimeout = tcpReadTimeout
            s
        }
        catch (e: Exception) {
            null
        }
    }

    protected suspend fun connectTcpToRead(address: InetAddress, port: Int, token: AckToken): NetworkInputStream? {
        val cl = connectTcp(address, port) ?: return null

        val ins = NetworkInputStream(cl.getInputStream())
        val os = NetworkOutputStream(cl.getOutputStream(), null)

        token.toStream(os)

        os.close()

        return ins
    }

    protected suspend fun connectTcpToWrite(address: InetAddress, port: Int, token: AckToken): NetworkOutputStream? {
        val cl = connectTcp(address, port) ?: return null

        return NetworkOutputStream(cl.getOutputStream(), cl::close).also {
            token.toStream(it)
        }
    }

    /**
     * Отправка в сеть пакета обнаружения соседних ретрансляторов
     */
    protected fun sendHello(targets: Array<InetSocketAddress>) {
        val wr = BinaryWriter()

        wr.write(0xaa.toByte())
        wr.write(1.toByte())
        wr.write(relayID)

        val a = adjacentRelays

        wr.write(a.size.toByte())
        a.forEach(wr::write)

        val data = wr.toArray()

        targets.forEach { sendDatagram(data, it) }
    }

    /**
     * Отправка в сеть пакета оповещения о завершении работы ретранслятора
     */
    protected fun sendBye(targets: Array<InetSocketAddress>) {
        val wr = BinaryWriter()

        wr.write(0xaa.toByte())
        wr.write(2.toByte())
        wr.write(relayID)

        val data = wr.toArray()

        targets.forEach { sendDatagram(data, it) }
    }

    companion object {
        fun scoreIPAddress(address: InetAddress) = if (address is Inet6Address) address.scopeId else 0
    }

    /**
     * Чтение широковещательного сокета
     */
    protected fun beginReceiveMulticast(socket: MulticastSocket, receiver: (ByteArray, InetSocketAddress) -> Unit) {
        launch {
            val buf = ByteArray(16384)

            while (isActive) {
                val dp = DatagramPacket(buf, buf.size)

                try {
                    socket?.receive(dp)
                }
                catch (e: SocketException) {
                    if (isActive)
                        logger.writeLog(LogImportance.Error, logCat, "Multicast::receive: ${e.message}")
                }

                val l = dp.length

                if (l == 0 || l == buf.size)
                    continue

                val ba = ByteArray(l)
                dp.data.copyInto(ba, endIndex = l)

                receiver(ba, dp.socketAddress as InetSocketAddress)
            }
        }
    }

    fun addRemotePeer(remotePeer: InetSocketAddress) {
        remotePeers.add(remotePeer)

        scanInterfaces()
    }

    protected abstract fun scanInterfaces()

    protected open val tcpListenEnabled = true
}