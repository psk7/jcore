package pvt.psk.jcore.network.commands

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

class HostAdmResolved(val host: HostID, val admPoint: InetSocketAddress) : Message() {

    override fun toString() = "HostAdmResolved $host->$admPoint"
}