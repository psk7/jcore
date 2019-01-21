package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

class PingReplyCommand(FromHost: HostID, ToHost: HostID, val Token: AckToken) : PeerCommand(CommandID.PingReply, FromHost, ToHost)
