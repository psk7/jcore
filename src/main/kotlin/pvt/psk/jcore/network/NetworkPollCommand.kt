package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*

class NetworkPollCommand : PollCommand(HostID.Local, HostID.Local) {

    override fun createHostInfoCommand(SeqID: Int, FromHost: HostID, ToHost: HostID): HostInfoCommand =
            HostInfoCommand(SeqID, FromHost, channels.map {
                create(it.key, (it.value as NetworkChannel).basePort, false, FromHost, true)
            }.toTypedArray(), ToHost)
}