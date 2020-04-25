package pvt.psk.jcore.relay

import pvt.psk.jcore.channel.*
import java.util.concurrent.*

class RelayMessage(val source: RelayID,
                   val targetRelay: RelayID,
                   val payload: Any,
                   val ttl: UInt = 8u,
                   relayTokens: IntArray? = null) : Message() {

    private val relays = ConcurrentHashMap<Int, Unit>()

    fun isRelayedThrough(relayID: RelayID) = relays.containsKey(relayID.id)

    val relaysTokens: IntArray
        get() = relays.keys().toList().toIntArray()
}