package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*

class EndPointInfo(val target: HostID, val channelName: String, val readOnly: Boolean, private vararg val payload: Any)
{
    fun get(Pos: Int): Any = payload[Pos]
}