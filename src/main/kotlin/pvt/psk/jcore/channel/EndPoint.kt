package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

open class EndPoint(val dataChannel: IChannel, val sender: SenderSocket, val targetHost: HostID) {
}