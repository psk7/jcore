package pvt.psk.jcore.channel

import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*

abstract class SenderSocket(val cancellationToken: CancellationToken, val logger: Logger?) : ISender {

    val logCat: String = "SenderSocket"

    init {
        cancellationToken.register(::safeClose)
    }

    override fun send(Packet: DataPacket, Target: EndPoint) {

        if (!Packet.ToHost.isNetwork)
            return

        when (Packet) {
            is BytesPacket -> sendDatagram(Packet, Target)
        }
    }

    protected abstract fun sendDatagram(Data: BytesPacket, Target: EndPoint)

    protected abstract fun safeClose()
}