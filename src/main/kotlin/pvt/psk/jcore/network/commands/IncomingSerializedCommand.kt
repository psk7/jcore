package pvt.psk.jcore.network.commands

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.net.*

class IncomingSerializedCommand(val data: ByteArray, val from: InetSocketAddress) : Message() {

    override fun toString() = "IncomingSerializedCommand Length=${data.size}, Sender=$from"
}