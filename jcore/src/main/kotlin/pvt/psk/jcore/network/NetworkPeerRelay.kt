package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*
import java.net.*

class NetworkPeerRelay(relayID: RelayID, admPort: Int, private val peer: InetSocketAddress) :
    BaseNetworkRelay(relayID) {

    val mcastudp: MulticastSocket
    val ucastudp: SafeUdpClient
    val tcp: SafeTCPCallbackListener

    val any: InetAddress = when (peer.address) {
        is Inet4Address -> Inet4Address.getByName("0.0.0.0")
        is Inet6Address -> Inet6Address.getByName("::")
        else            -> throw IllegalArgumentException()
    }

    init {

        fun r(b: ByteArray, f: InetSocketAddress) {

            if (f.address is Inet6Address && peer.address !is Inet6Address)
                return

            if (f.address is Inet4Address && peer.address !is InetAddress)
                return

            launch { received(b, f) }
        }

        mcastudp = MulticastSocket(InetSocketAddress(any, admPort))
        ucastudp = udpFactory.create(InetSocketAddress(any, 0), ::r)

        tcp = SafeTCPCallbackListener(any, 0)

        beginReceiveMulticast(mcastudp, ::r)

        GlobalScope.launch(Dispatchers.IO) { scanInterfaces() }
    }

    override fun sendDatagram(data: ByteArray, target: InetSocketAddress) = ucastudp.send(data, target)

    override fun sendStream(msg: StreamPacket): (BinaryWriter) -> Unit =
        if (tcpListenEnabled) sendStreamPassive(msg, tcp.localEndPoint.port) else sendStreamActive(msg)

    override fun serializeTcpListenerPort(writer: BinaryWriter) = writer.write(tcp.localEndPoint.port.toUShort())

    override fun scanInterfaces() = sendHello(arrayOf(peer))
}