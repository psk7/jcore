@file:Suppress("UNUSED_VARIABLE")

package pvt.psk.jcore.relay

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

class RoutingTableTest {

    @Test
    fun sequence() {
        val r1 = RelayID(1)
        val r2 = RelayID(2)
        val r3 = RelayID(3)

        val h1 = HostID(1)
        val h2 = HostID(2)
        val h3 = HostID(3)

        val rt = RoutingTable(r1)

        assertTrue(rt.updateRelayInfo(RelayInfo(r2, arrayOf(h2), arrayOf(r1), AckToken.empty, 2)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r2, arrayOf(h2, h3), arrayOf(r1, r3), AckToken.empty, 1)))
    }

    @Test
    fun Line2() {
        val r1 = RelayID(1)
        val r2 = RelayID(2)

        val h1 = HostID(1)
        val h2 = HostID(2)

        val rt = RoutingTable(r1)

        assertTrue(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2)))
        assertFalse(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r2, arrayOf(h2), arrayOf(r1), AckToken.empty, 2)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r2, arrayOf(h2), arrayOf(r1), AckToken.empty, 2)))

        assertEquals(r2, rt.findNextHopForHost(h2))
        assertEquals(r1, rt.findNextHopForHost(h1))
    }

    @Test
    fun Line3() {
        val r1 = RelayID(1)
        val r2 = RelayID(2)
        val r3 = RelayID(3)

        val h1 = HostID(1)
        val h3 = HostID(3)

        // R1 -- R2 -- R3

        val rt = RoutingTable(r1)

        assertTrue(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2)))
        assertFalse(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r2, arrayOf(), arrayOf(r1, r3), AckToken.empty, 2)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r2, arrayOf(), arrayOf(r1, r3), AckToken.empty, 2)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r3, arrayOf(h3), arrayOf(r2), AckToken.empty, 2)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r3, arrayOf(h3), arrayOf(r2), AckToken.empty, 2)))

        assertEquals(r2, rt.findNextHopForHost(h3))
    }

    @Test
    fun Line4() {
        val r1 = RelayID(1)
        val r2 = RelayID(2)
        val r3 = RelayID(3)
        val r4 = RelayID(4)

        val h1 = HostID(1)
        val h4 = HostID(4)

        val rt = RoutingTable(r1)

        // R1 -- R2 -- R3 -- R4

        assertTrue(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2)))
        assertFalse(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r2, arrayOf(), arrayOf(r1, r3), AckToken.empty, 2)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r2, arrayOf(), arrayOf(r1, r3), AckToken.empty, 2)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r3, arrayOf(), arrayOf(r2, r4), AckToken.empty, 3)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r3, arrayOf(), arrayOf(r2, r4), AckToken.empty, 3)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r4, arrayOf(h4), arrayOf(r3), AckToken.empty, 4)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r4, arrayOf(h4), arrayOf(r3), AckToken.empty, 4)))

        assertEquals(r2, rt.findNextHopForHost(h4))

        //  /----------------\
        // R1 -- R2 -- R3 -- R4

        assertTrue(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2, r4)))
        assertFalse(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2, r4)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r4, arrayOf(h4), arrayOf(r3, r1), AckToken.empty, 5)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r4, arrayOf(h4), arrayOf(r3, r1), AckToken.empty, 5)))

        assertEquals(r4, rt.findNextHopForHost(h4))
    }

    @Test
    fun Nodes4() {
        val r1 = RelayID(1)
        val r2 = RelayID(2)
        val r3 = RelayID(3)
        val r4 = RelayID(4)

        val h1 = HostID(1)
        val h4 = HostID(4)

        val rt = RoutingTable(r1)

        //          R3
        //        /   |
        // R1 -- R2   |
        //        \   |
        //          R4

        assertTrue(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2)))
        assertFalse(rt.updateRelayInfo(arrayOf(h1), arrayOf(r2)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r2, arrayOf(), arrayOf(r1, r3, r4), AckToken.empty, 2)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r2, arrayOf(), arrayOf(r1, r3, r4), AckToken.empty, 2)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r3, arrayOf(), arrayOf(r2, r4), AckToken.empty, 3)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r3, arrayOf(), arrayOf(r2, r4), AckToken.empty, 3)))

        assertTrue(rt.updateRelayInfo(RelayInfo(r4, arrayOf(h4), arrayOf(r2, r3), AckToken.empty, 4)))
        assertFalse(rt.updateRelayInfo(RelayInfo(r4, arrayOf(h4), arrayOf(r2, r3), AckToken.empty, 4)))

        assertEquals(r2, rt.findNextHopForHost(h4))
    }

    @Test
    fun expand() {
        val r1 = RelayID(1)
        val r2 = RelayID(2)
        val r3 = RelayID(3)
        val r4 = RelayID(4)

        val h1 = HostID(1)
        val h2 = HostID(2)
        val h3 = HostID(3)
        val h4 = HostID(4)

        var rt = RoutingTable(r1)

        rt.updateRelayInfo(arrayOf(h1), arrayOf(r2))
        rt.updateRelayInfo(RelayInfo(r2, arrayOf(h2), arrayOf(r1), AckToken.empty, 2))

        var res = rt.expand(h1, HostEndpointIDSet(arrayOf(HostEndpointID(h2, 0.toUShort())))).toTypedArray()

        assertEquals(1, res.size)
        assertEquals(r2, res[0].first)
        var t = res[0].second.toTypedArray()
        assertEquals(1, t.size)
        assertEquals(h2, t[0].hostID)

        // --------------------

        rt = RoutingTable(r1)

        rt.updateRelayInfo(arrayOf(h1), arrayOf(r2, r3));
        rt.updateRelayInfo(RelayInfo(r2, arrayOf(h2), arrayOf(r1, r2, r3, r4), AckToken.empty, 2))
        rt.updateRelayInfo(RelayInfo(r3, arrayOf(h3), arrayOf(r1, r2, r3, r4), AckToken.empty, 3))
        rt.updateRelayInfo(RelayInfo(r4, arrayOf(h4), arrayOf(r2, r3, r4), AckToken.empty, 4))

        res = rt.expand(h1,
                        HostEndpointIDSet(arrayOf(HostEndpointID(h2, 0U), HostEndpointID(h3, 0U),
                                                  HostEndpointID(h4, 0U))))
            .toTypedArray()

        assertEquals(2, res.size)
        assertEquals(r2, res[0].first)
        assertEquals(r3, res[1].first)

        t = res.first { x -> x.first == r2 }.second.toTypedArray()
        if (t.size == 2) {
            assertTrue(t.contains(HostEndpointID(h2, 0U)))
            assertTrue(t.contains(HostEndpointID(h4, 0U)))
        } else if (t.size == 1)
            assertTrue(t.contains(HostEndpointID(h2, 0U)))
        else fail()

        t = res.first { x -> x.first == r3 }.second.toTypedArray()
        if (t.size == 2) {
            assertTrue(t.contains(HostEndpointID(h3, 0U)))
            assertTrue(t.contains(HostEndpointID(h4, 0U)))
        } else if (t.size == 1)
            assertTrue(t.contains(HostEndpointID(h3, 0U)))
        else
            fail()
    }
}