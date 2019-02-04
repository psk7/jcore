package pvt.psk.jcore.network

import pvt.psk.jcore.channel.*

class NetworkChannel : BaseChannel() {

    val networkLocalEndPoint: NetworkEndPoint
        get() = super.localEndPoint as NetworkEndPoint

}