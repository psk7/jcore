package pvt.psk.jcore.channel

import pvt.psk.jcore.host.HostID

abstract class Message(val FromHost: HostID, val ToHost: HostID) { }