package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*

class DiscoveryCommand(FromHost: HostID, ToHost: HostID) : PeerCommand(CommandID.Discovery, FromHost, ToHost){

    override fun toString() = "Discovery: I am '$FromHost'"
}
