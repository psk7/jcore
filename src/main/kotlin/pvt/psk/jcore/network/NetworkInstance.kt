package pvt.psk.jcore.network

import io.ktor.util.*
import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.instance.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.network.commands.*

open class NetworkInstance(Name: String, DomainName: String, AdmPort: Int, Log: Logger?)
    : BaseInstance(Name, DomainName, AdmPort, Log) {

    private val _directory = IPAddressDirectory()
    private lateinit var _cf: NetworkCommandFactory

    override fun createPeerProtocol(Control: IChannel, Domain: String): PeerProtocol {

        _cf = NetworkCommandFactory(selfHostID, DomainName, Control)

        return NetworkPeerProtocol(selfHostID, Control, Log)
    }

    override fun createPeerCommandSocket(): PeerCommandSocket =
            NetworkCommandSocket(controlBus, AdmPort, Log, _directory, CancellationToken)

    override fun createChannel(channelName: String, channelRouter: Router): BaseChannel =
            NetworkChannel(channelName, controlBus, channelRouter, selfHostID, _directory, Log, pvt.psk.jcore.utils.CancellationToken.None)

    override fun createPollCommand(): PollCommand = NetworkPollCommand()

    /**
     * Повторное сканирование и настройка сетевых интерфейсов
     */
    open fun rescan() {
        (ComSocket as? NetworkCommandSocket)?.scan()

        controlBus.sendMessage(RebindCommand())
    }
}