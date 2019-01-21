package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

class PingCommand(FromHost: HostID, ToHost: HostID, val Token: AckToken) : PeerCommand(CommandID.Ping, FromHost, ToHost)
