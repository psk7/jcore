package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import java.net.*
import java.util.concurrent.*

class IPEndPointsList {

    private class Bag {
        var EndPoint: EndPoint? = null
        val Completion = CompletableDeferred<Unit>()
    }

    private val _l = ConcurrentHashMap<InetSocketAddress, Bag>()

    fun find(IP: InetSocketAddress): Deferred<EndPoint> {

        val bag = _l.getOrPut(IP) { Bag() }

        return GlobalScope.async(Dispatchers.Unconfined) {
            bag.Completion.await()

            return@async bag.EndPoint!!
        }
    }

    fun found(IP: InetSocketAddress, endPoint: EndPoint) {

        val bag = _l.getOrPut(IP) { Bag() }

        bag.EndPoint = endPoint

        bag.Completion.complete(Unit)
    }
}