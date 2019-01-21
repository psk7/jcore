package pvt.psk.jcore.network

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.nio.channels.*

class NetworkSenderSocket(cancellationToken: CancellationToken, logger: Logger) : SenderSocket(cancellationToken, logger) {
    enum class PacketID(val id: Int) {

        Datagram(1),
        Stream(2),
        Ping(3),
        PingReply(4),
    }
    val _udp = AsynchronousSocketChannel.open()

    private suspend fun beginReceive() {
    }

    override fun sendDatagram(Data: BytesPacket, Target: EndPoint) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun safeClose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
