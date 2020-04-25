package pvt.psk.jcore.utils

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.relay.*
import java.net.*

class ConcurrentPriorityHashMapTest {

    @Test
    fun `remove values from collection`() {
        val rid1 = RelayID.new()
        val rid2 = RelayID.new()

        val c = ConcurrentPriorityHashMap<RelayID, InetSocketAddress>()

        val a1 = InetSocketAddress(Inet6Address.getByName("::1"), 1000)
        val a2 = InetSocketAddress(Inet6Address.getByName("::2"), 1000)
        val a3 = InetSocketAddress(Inet6Address.getByName("::3"), 1000)

        c.set(rid1, 1, a1)
        c.set(rid1, 2, a2)
        c.set(rid1, 3, a3)

        c.set(rid2, 1, a1)
        c.set(rid2, 2, a2)
        c.set(rid2, 3, a3)

        assertEquals(2, c.keys.size)

        c.removeValue(a1)

        assertEquals(2, c.keys.size)

        assertEquals(a2, c[rid1])
        assertEquals(a2, c[rid2])

        c.removeValue(a2)

        assertEquals(2, c.keys.size)

        assertEquals(a3, c[rid1])
        assertEquals(a3, c[rid2])

        c.removeValue(a3)

        assertEquals(2, c.keys.size)

        assertEquals(null, c[rid1])
        assertEquals(null, c[rid2])
    }

    @Test
    fun `group by`() {
        val c = ConcurrentPriorityHashMap<HostID, RelayID>()

        val h1 = HostID.new("")
        val h2 = HostID.new("")
        val h3 = HostID.new("")

        val r1 = RelayID.new()
        val r2 = RelayID.new()
        val r3 = RelayID.new()

        c.set(h1, 1, r1)
        c.set(h1, 2, r2)
        c.set(h1, 3, r3)

        c.set(h2, 1, r1)
        c.set(h2, 2, r2)
        c.set(h2, 3, r3)

        c.set(h3, 1, r1)
        c.set(h3, 2, r2)
        c.set(h3, 3, r3)

        val g = c.groupByValues { it == h1 || it == h2 }

        assertEquals(1, g.size)

        assertEquals(2, g[r1]?.size)
        assertTrue(g[r1]?.toSet()?.contains(h1) ?: false)
        assertTrue(g[r1]?.toSet()?.contains(h2) ?: false)
    }
}