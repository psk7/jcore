package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.instance.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.CancellationToken
import java.net.*

open class NetworkInstance(Name: String, DomainName: String, AdmPort: Int, Log: Logger?) : BaseInstance(Name, DomainName, AdmPort, Log) {

    override fun createPeerProtocol(Control: IChannel, Domain: String): PeerProtocol = NetworkPeerProtocol(HostID, Domain, Control, Log)

    override fun createPeerCommandSocket(): PeerCommandSocket =
        NetworkCommandSocket(ControlBus, AdmPort, Log, PeerProto!!, CancellationToken).apply {
            IgnoreFromHost = HostID
        }

    override fun createChannel(channelName: String, channelRouter: Router): BaseChannel =
        NetworkChannel(channelName, PeerProto!!, ControlBus, channelRouter, HostID, Log, InetSocketAddress(Inet6Address.getByName("::"), 0),
                       pvt.psk.jcore.utils.CancellationToken.None)

    override fun createPollCommand(): PollCommand = NetworkPollCommand()
}