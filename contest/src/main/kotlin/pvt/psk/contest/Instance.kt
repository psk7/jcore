package pvt.psk.contest

import pvt.psk.jcore.network.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*

class Instance(Name: String) : NetworkInstance(Name, "ConTest") {

    init {
        relay = NetworkRelay(RelayID.new(), AddressFamily.IPv6, 3447)
    }
}