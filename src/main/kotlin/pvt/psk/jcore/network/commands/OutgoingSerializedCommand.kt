package pvt.psk.jcore.network.commands

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

class OutgoingSerializedCommand(val data: ByteArray,
                                val toHost: HostID,
                                val toEndPoint: InetSocketAddress?,
                                val isBroadcast: Boolean = false) : Message() {

    override fun toString() = "OutgoingSerializedCommand Length=${data.size}, To=$toEndPoint"
}