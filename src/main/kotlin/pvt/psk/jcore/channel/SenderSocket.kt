package pvt.psk.jcore.channel

import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*

abstract class SenderSocket(cancellationToken: CancellationToken, val logger: Logger?) : ISender {

    val logCat: String = "SenderSocket"

    init {
        cancellationToken.register(::safeClose)
    }

    /**
     * Отправка сообщения адресату
     */
    override fun send(Packet: DataPacket, Target: EndPoint) {

        if (!Packet.toHost.isNetwork)
            return

        when (Packet) {
            is BytesPacket -> sendDatagram(Packet, Target)
        }
    }

    /**
     * Отправка байтовой посылки адресату
     */
    protected abstract fun sendDatagram(data: BytesPacket, Target: EndPoint)

    protected abstract fun safeClose()
}