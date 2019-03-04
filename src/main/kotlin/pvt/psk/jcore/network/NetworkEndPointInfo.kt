package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*

fun create(channelName: String, port: Int, readOnly: Boolean, target: HostID, canReceiveStream: Boolean) =
    EndPointInfo(target, channelName, readOnly, port, canReceiveStream)

val EndPointInfo.port: Int
    get() = get(0) as Int

val EndPointInfo.canReceiveStream: Boolean
    get() = get(1) as Boolean