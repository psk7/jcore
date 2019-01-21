package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*

class PingReply(FromHost: HostID, ToHost: HostID) : PeerCommand(CommandID.PingReply, FromHost, ToHost)
