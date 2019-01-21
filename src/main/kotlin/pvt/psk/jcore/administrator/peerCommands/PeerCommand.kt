package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*

abstract class PeerCommand(val CommandID: CommandID, FromHost: HostID, ToHost: HostID) : Message(FromHost, ToHost)
