package pvt.psk.jcore.administrator

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.util.concurrent.*

/**
 * Команда опроса конечных точек хоста для сбора информации о каналах
 */
abstract class PollCommand : Message() {

    protected val chans = ConcurrentHashMap<String, BaseChannel>()

    val channels
        get() = chans.map { Pair(it.key, it.value) }.toTypedArray()

    abstract fun createHostInfoCommand(FromHost: HostID, ToHost: HostID): HostInfoCommand

    fun registerChannel(ChannelName: String, Channel: BaseChannel) {
        chans[ChannelName] = Channel
    }
}