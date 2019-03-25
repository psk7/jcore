package pvt.psk.jcore.network.commands

import io.ktor.util.*
import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.network.*

class NetworkPollCommand : PollCommand() {

    override fun createHostInfoCommand(FromHost: HostID, ToHost: HostID): HostInfoCommand =
            HostInfoCommand(FromHost, chans.map {
                create(it.key, (it.value as NetworkChannel).basePort, it.value.acceptTags, FromHost, true)
            }.toTypedArray(), ToHost)
}