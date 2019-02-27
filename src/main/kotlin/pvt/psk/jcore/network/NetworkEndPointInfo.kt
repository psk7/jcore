package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*

fun create(channelName: String, port: Int, readOnly: Boolean, target: HostID): EndPointInfo {
    return EndPointInfo(target, channelName, readOnly, port)
}

val EndPointInfo.port: Int
    get() = 0