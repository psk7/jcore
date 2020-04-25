package pvt.psk.jcore.administrator

import pvt.psk.jcore.relay.*

data class EndPointInfo(val target: HostEndpointID, val channelName: String, val acceptTags: Array<PacketTag>)
