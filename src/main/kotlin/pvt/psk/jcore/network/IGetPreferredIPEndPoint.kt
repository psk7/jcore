package pvt.psk.jcore.network

import java.net.*

interface IGetPreferredIPEndPoint {
    fun get(Source: Array<InetSocketAddress>): InetSocketAddress
}