package pvt.psk.jcore.network

import kotlinx.coroutines.*
import org.koin.core.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*
import java.net.*

open class NetworkRelay(relayID: RelayID,
                        addressFamily: AddressFamily,
                        admPort: Int) : BaseNetworkRelay(relayID) {

    companion object {
        private val disabledNetworkNames = arrayOf("radio", "p2p")

        val multicastAddress = InetAddress.getByName("FF02::1")
    }

    private val debugHolder: DebugInfoHolder by inject()

    val any: InetAddress = when (addressFamily) {
        AddressFamily.IPv4 -> Inet4Address.getByName("0.0.0.0")
        AddressFamily.IPv6 -> Inet6Address.getByName("::")
    }

    val scan = when (addressFamily) {
        AddressFamily.IPv4 -> ::scan4
        AddressFamily.IPv6 -> ::scan6
    }

    protected val mcastudp: MulticastSocket
    protected val ucastudp: SafeUdpClient
    protected val tcp: SafeTCPCallbackListener

    protected var broadcasts: Array<InetSocketAddress>? = null

    init {

        fun r(b: ByteArray, f: InetSocketAddress) {

            if (f.address is Inet6Address && addressFamily != AddressFamily.IPv6)
                return

            if (f.address is Inet4Address && addressFamily != AddressFamily.IPv4)
                return

            launch { received(b, f) }
        }

        var ap = admPort

        mcastudp = when (addressFamily) {
            AddressFamily.IPv4 -> MulticastSocket(InetSocketAddress(any, ap))
            AddressFamily.IPv6 -> MulticastSocket(ap)
        }

        if (ap == 0)
            ap = mcastudp.localPort

        iAdmPort = ap

        ucastudp = udpFactory.create(InetSocketAddress(any, 0), ::r)

        tcp = SafeTCPCallbackListener(any, 0)

        beginReceiveMulticast(mcastudp, ::r)

        launch(Dispatchers.IO) { scanInterfaces() }
    }

    override fun scanInterfaces() {
        ucastudp.rebind()
        scan()
    }

    private fun scan6() {
        val ifcs = interfaces

        ifcs.forEach { (num, iface) ->
            try {
                mcastudp.joinGroup(InetSocketAddress(multicastAddress, admPort), iface)
            }
            catch (e: SocketException) {
                logger.writeLog(LogImportance.Error, logCat, "scan6::${e.message.toString()}")
            }
        }

        val brcst = ifcs.map { InetSocketAddress(InetAddress.getByName("[FF02::1%${it.key}]"), admPort) }
            .union(remotePeers)
            .toTypedArray()

        broadcasts = brcst

        fun checkScope(addr: InetAddress): Boolean {
            if (addr !is Inet6Address)
                return true

            return addr.scopeId == 0 || ifcs.keys.contains(addr.scopeId)
        }

        // Очистка неактуальных маршрутов
        for (r in relays.values.filter { !checkScope(it.address) })
            relays.removeValue(r)

        debugHolder.broadcasts = brcst

        sendHello(brcst)
    }

    private fun scan4() {
        val ifcs = interfaces

        val brcst = ifcs
            .flatMap { it.value.interfaceAddresses.map { a -> a.broadcast } }
            .filterNotNull()
            .distinct()
            .filter { it !is Inet6Address && !it.isLoopbackAddress }
            .map { InetSocketAddress(it, admPort) }
            .union(remotePeers)
            .toTypedArray()

        broadcasts = brcst

        debugHolder.broadcasts = brcst

        // Очистка неактуальных маршрутов
        sendHello(brcst)
    }

    /**
     * Отправка в сеть пакета обнаружения соседних ретрансляторов
     */
    fun sendHello() {
        sendHello(broadcasts ?: return)
    }

    /**
     * Отправка в сеть пакета оповещения о завершении работы ретранслятора
     */
    fun sendBye() {
        sendBye(broadcasts ?: return)
    }

    private val interfaces
        get() =
            NetworkInterface.getNetworkInterfaces().asSequence().filter {
                !it.isLoopback && it.isUp && it.supportsMulticast() && !it.isPointToPoint && !disabledNetworkNames.any { dn ->
                    it.name.contains(dn, true)
                }
            }.associateBy { it.index }

    /**
     * Отправка байтовой посылки в сеть
     *
     * @param data Сообщение
     * @param target IP адрес ретранслятора цели
     */
    override fun sendDatagram(data: ByteArray, target: InetSocketAddress) = ucastudp.send(data, target)

    /**
     * Отправка отдельного потока
     */
    override fun sendStream(msg: StreamPacket): (BinaryWriter) -> Unit =
        if (tcpListenEnabled) sendStreamPassive(msg, tcp.localEndPoint.port) else sendStreamActive(msg)

    override fun serializeTcpListenerPort(writer: BinaryWriter) = writer.write(tcp.localEndPoint.port.toUShort())

    override fun close() {
        // Multicast завершается первым и принудительно, т.к. его читатель ждет в блокирующем режиме сокета -> SupervisorJob не завершится сам
        mcastudp.close()

        repeat(3) {
            sendBye()
        }

        super.close()
        ucastudp.close()
    }
}