package pvt.psk.jcore.network

import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.network.commands.*
import pvt.psk.jcore.utils.*

@KtorExperimentalAPI
@ExperimentalCoroutinesApi
class NetworkPeerProtocol(selfHostID: HostID, controlChannel: IChannel, logger: Logger?) :
        PeerProtocol(selfHostID, controlChannel, logger) {

    private val mtx = Mutex()

    override fun onControlReceived(command: PeerCommand) {

        if (!command.toHost.isLocal)
            return

        logger?.writeLog(LogImportance.Trace, logCat, "Принята команда $command для ${command.toHost}")

        if (command is NetworkPingReplyCommand)
            command.token.received(command.from)

        GlobalScope.launch(Dispatchers.Unconfined) {
            mtx.lock()

            try {
                if (!filter(command))
                    return@launch

                super.onControlReceived(command)
            } finally {
                command.complete()

                mtx.unlock()
            }
        }
    }

    override fun createPollCommand(): PollCommand = NetworkPollCommand()
}