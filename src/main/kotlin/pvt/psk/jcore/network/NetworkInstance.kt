package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.instance.*
import pvt.psk.jcore.logger.*
import java.net.*

open class NetworkInstance(Name: String, DomainName: String, AdmPort: Int, Log: Logger?) : BaseInstance(Name, DomainName, AdmPort, Log) {

    override fun createPeerProtocol(Control: IChannel, Domain: String): PeerProtocol = NetworkPeerProtocol(selfHostID, Domain, Control, Log)

    override fun createPeerCommandSocket(): PeerCommandSocket =
        NetworkCommandSocket(ControlBus, AdmPort, Log, PeerProto!!, CancellationToken).apply {
            IgnoreFromHost = selfHostID
        }

    override fun createChannel(channelName: String, channelRouter: Router): BaseChannel =
        NetworkChannel(channelName, PeerProto!!, ControlBus, channelRouter, selfHostID, Log, InetSocketAddress(Inet6Address.getByName("::"), 0),
                       pvt.psk.jcore.utils.CancellationToken.None)

    override fun createPollCommand(): PollCommand = NetworkPollCommand()
}