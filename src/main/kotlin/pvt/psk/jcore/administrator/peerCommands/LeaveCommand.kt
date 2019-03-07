package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*

class LeaveCommand(FromHost: HostID, ToHost: HostID) : PeerCommand(CommandID.Leave, FromHost, ToHost) {
    override fun toString(): String = "Leave Host=$FromHost"
}
