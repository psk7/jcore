package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.instance.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.CancellationToken



open class NetworkInstance(Name: String, DomainName: String, AdmPort: Int, Log: Logger?) : BaseInstance(Name, DomainName, AdmPort, Log) {

    override fun createPeerProtocol(Control: IChannel, Domain: String): PeerProtocol = NetworkPeerProtocol(HostID, Domain, Control, Log)

    override fun createPeerCommandSocket(): PeerCommandSocket {
        val cs = NetworkCommandSocket(ControlBus.getChannel(), AdmPort, Log, PeerProto, CancellationToken)
        cs.IgnoreFromHost = HostID

        //cs.BeginReceive()

        return cs
    }
}