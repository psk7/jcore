package pvt.psk.jcore.channel

import kotlinx.coroutines.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import kotlin.coroutines.*

abstract class SenderSocket(cancellationToken: CancellationToken, val logger: Logger?) : CoroutineScope, ISender {

    val logCat: String = "SenderSocket"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Unconfined

    init {
        cancellationToken.register(::safeClose)
    }

    /**
     * Отправка сообщения адресату
     */
    override fun send(packet: DataPacket, target: EndPoint) {

        if (!packet.toHost.isNetwork)
            return

        when (packet) {
            is BytesPacket -> sendDatagram(packet, target)
        }
    }

    /**
     * Отправка байтовой посылки адресату
     */
    protected abstract fun sendDatagram(data: BytesPacket, Target: EndPoint)

    protected abstract fun safeClose()
}