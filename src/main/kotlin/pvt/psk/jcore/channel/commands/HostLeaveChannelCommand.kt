package pvt.psk.jcore.channel.commands

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*

class HostLeaveChannelCommand(val leavedHost: HostID, val channel: String) : Message()