package pvt.psk.jcore.network

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*
import utils.*
import java.util.*

class NetworkRelayTest : JcoreKoinTest() {

    class TestNetworkRelay(val active: Boolean,
                           relayID: RelayID,
                           addressFamily: AddressFamily,
                           admPort: Int) :
        NetworkRelay(relayID, addressFamily, admPort) {

        override val tcpListenEnabled = active
    }

    @Test
    @Timeout(10)
    fun sendStreamActive() {
        sendStream(true)
    }

    @Test
    @Timeout(10)
    fun sendStreamPassive() {
        sendStream(false)
    }

    fun sendStream(active: Boolean) {
        val rid1 = RelayID.new()
        val rid2 = RelayID.new()

        val hid1 = HostID.new("H1")
        val hid2 = HostID.new("H2")

        val he1 = HostEndpointID(hid1, 1U)
        val he2 = HostEndpointID(hid2, 2U)

        val rp = Random().nextInt(40000) + 10000
        val tcssp = CompletableDeferred<ByteArray>()

        val r1 = TestNetworkRelay(true, rid1, AddressFamily.IPv6, 0)
        val r2 = TestNetworkRelay(active, rid2, AddressFamily.IPv6, r1.admPort)

        var ff = false

        r1.addAdjacentHost(hid1, { e ->
            val bp = e.payload as? BytesPacket ?: return@addAdjacentHost

            tcssp.complete(bp.data)
        })

        r2.addAdjacentHost(hid2, {})

        while (!r1.adjacentRelays.contains(rid2))
            runBlocking { delay(10) }

        while (!r2.adjacentRelays.contains(rid1))
            runBlocking { delay(10) }

        // Теперь r1 и r2 знают друг о друге

        while (!r1.remoteHosts.contains(hid2))
            runBlocking { delay(10) }

        while (!r2.remoteHosts.contains(hid1))
            runBlocking { delay(10) }

        // Теперь r1 и r2 знают о хостах друг друга

        val b = ByteArray(16384)
        Random().nextBytes(b)

        val bp = BytesPacket(b)

        r2.send(RelayEnvelope(he2, arrayOf(he1), bp));

        val rb = runBlocking { tcssp.await() }

        rb.forEachIndexed { i, bb -> assertEquals(b[i], bb) }
    }
}