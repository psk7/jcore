package network

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.network.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.util.*

class IPEndPointsListTest {

    private class Sender : ISender {
        override fun send(Packet: DataPacket, Target: EndPoint) {}
    }

    @Test
    fun reFound() {

        val cr = Router()
        val rd = Router()
        val remote1 = HostID(UUID.randomUUID(), "Remote1")
        val remote2 = HostID(UUID.randomUUID(), "Remote2")

        val ip1 = InetSocketAddress(InetAddress.getByName("::1"), 1000)
        val nep1 = NetworkEndPoint(rd, Sender(), remote1, cr)
        val nep2 = NetworkEndPoint(rd, Sender(), remote2, cr)

        val l = IPEndPointsList()

        assertFalse(l.find(ip1).wait(1)) // Выход по времени ожидания. Точка не найдена.

        l.found(ip1, nep1)

        assertTrue(l.find(ip1).wait(1)) // Точка найдена.

        assertEquals(nep1, l.find(ip1).result)

        l.found(ip1, nep2)

        assertTrue(l.find(ip1).wait(1)) // Точка найдена.
        assertEquals(nep2, l.find(ip1).result)
    }

    @Test
    fun testWait() {

        val cr = Router()
        val rd = Router()
        val remote1 = HostID(UUID.randomUUID(), "Remote1")

        val ip1 = InetSocketAddress(InetAddress.getByName("::1"), 1000)
        val nep1 = NetworkEndPoint(rd, Sender(), remote1, cr)

        val l = IPEndPointsList()

        var wt = l.find(ip1)

        assertFalse(wt.isCompleted)

        l.found(ip1, nep1)

        wt.wait()

        assertTrue(wt.isCompleted)

        // Повторный запрос сразу дает завершенную задачу
        wt = l.find(ip1)

        assertTrue(wt.isCompleted)
    }
}