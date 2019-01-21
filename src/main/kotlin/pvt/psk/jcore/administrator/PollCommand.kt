package pvt.psk.jcore.administrator

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.util.concurrent.*


abstract class PollCommand(FromHost: HostID, ToHost: HostID) : Message(FromHost, ToHost) {
    val _chans = ConcurrentHashMap<String, BaseChannel>()

    abstract fun createHostInfoCommand(SeqID: Int, FromHost: HostID, ToHost: HostID): HostInfoCommand

    fun registerChannel(ChannelName: String, Channel: BaseChannel) {
        _chans[ChannelName] = Channel
    }
}