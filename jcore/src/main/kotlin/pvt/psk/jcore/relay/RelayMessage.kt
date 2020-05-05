package pvt.psk.jcore.relay

import pvt.psk.jcore.channel.*

class RelayMessage(val source: RelayID, val targetRelay: RelayID, val payload: Any, val ttl: UInt) : Message()