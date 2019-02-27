package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

open class EndPoint(val dataChannel: IChannel, val sender: ISender, val targetHost: HostID) {

    private var isClosed = false

    fun close() {
        isClosed = true
    }
}