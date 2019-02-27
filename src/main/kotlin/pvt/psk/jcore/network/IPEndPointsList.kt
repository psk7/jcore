package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import java.net.*
import java.util.concurrent.*

class IPEndPointsList {

    val _l = ConcurrentHashMap<InetSocketAddress, EndPoint>()

    fun find(IP: InetSocketAddress): Deferred<EndPoint> {
        return CompletableDeferred()
    }

    fun found(IP: InetSocketAddress, endPoint: EndPoint) {

    }
}