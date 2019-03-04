package pvt.psk.jcore.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*

class NetworkSenderSocket(private val udp: SafeUdpClient, cancellationToken: CancellationToken, logger: Logger?) :
    SenderSocket(cancellationToken, logger) {

    enum class PacketID(val id: Int) {

        Datagram(1),
        Stream(2),
        Ping(3),
        PingReply(4),
    }

    val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("::", 0))

    init {
        GlobalScope.launch { beginAccept() }

        udp.received += { b, from -> udpReceived(b, from) }
    }

    private suspend fun beginAccept() {

        val s = tcp.accept()

        beginAccept()

        val rc = s.openReadChannel()

        try {
            val t = AckToken(BinaryReader(rc))

            t.received(rc)
        }
        catch (e: Exception) {
            return
        }
    }

    private fun udpReceived(bytes: ByteArray, from: InetSocketAddress) {

        val rd = BinaryReader(bytes)

        var b = rd.readByte()

        val pid = PacketID.values().find { it.id == b.toInt() }

        when (pid) {
            PacketID.Datagram -> receiveDatagram(rd, from)
            PacketID.Ping     -> {
            }
        }
    }

    private fun receiveDatagram(reader: BinaryReader, from: InetSocketAddress){

    }

    override fun sendDatagram(Data: BytesPacket, Target: EndPoint) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun safeClose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
