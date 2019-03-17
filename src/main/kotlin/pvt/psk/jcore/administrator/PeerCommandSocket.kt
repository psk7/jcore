package pvt.psk.jcore.administrator

import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*

@ExperimentalCoroutinesApi
abstract class PeerCommandSocket constructor(Bus: IChannel,
                                             val Log: Logger?,
                                             @Suppress("UNUSED_PARAMETER") CancellationToken: CancellationToken) {

    protected val bus: IChannelEndPoint

    init {
        bus = Bus.getChannel(::onBusCmd)
    }

    protected val logCat: String = "PeerCmd"

    protected abstract fun onBusCmd(channel: IChannelEndPoint, data: Message)

    protected fun onReceive(Message: Message) {
        Log?.writeLog(LogImportance.Trace, logCat, "Принята команда $Message")

        bus.sendMessage(Message)
    }

    abstract fun beginReceive()
}