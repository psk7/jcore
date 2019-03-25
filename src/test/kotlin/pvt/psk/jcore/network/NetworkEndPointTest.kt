package pvt.psk.jcore.network

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.util.*

class NetworkEndPointTest {

    private class Sender : ISender {
        val queue = LinkedList<Pair<DataPacket, EndPoint>>()

        override fun send(packet: DataPacket, target: EndPoint) = queue.push(Pair(packet, target))
    }

    @Test
    fun checkAcceptTags() {
        // Тест отправки пакетов с меткой

        val dr = Router()
        val s = Sender()

        val rh = HostID(UUID.randomUUID(), "RH1")

        // Отправка пакета без метки в конечную точку, у которой AcceptTags не установлен
        var nep = NetworkEndPoint(dr, s, rh, IPAddressDirectory(), 1234, null)
        dr.sendMessage(BytesPacket("Test1".toByteArray(), HostID.Local, rh))
        var (d, e) = s.queue.pop()
        assertSame(nep, e)
        assertEquals("Test1", String((d as BytesPacket).data))
        assertTrue(s.queue.size == 0)
        nep.close()

        fun sde(pair: Pair<DataPacket, EndPoint>) {
            d = pair.first; e = pair.second
        }

        // Отправка пакета без метки в конечную точку, у которой AcceptTags установлен но пуст
        nep = NetworkEndPoint(dr, s, rh, IPAddressDirectory(), 1234, arrayOf(""))
        dr.sendMessage(BytesPacket("Test2".toByteArray(), HostID.Local, rh))
        sde(s.queue.pop())
        assertSame(nep, e)
        assertEquals("Test2", String((d as BytesPacket).data))
        assertTrue(s.queue.size == 0)
        nep.close()

        // Отправка пакета без метки в конечную точку, у которой AcceptTags установлен 
        nep = NetworkEndPoint(dr, s, rh, IPAddressDirectory(), 1234, arrayOf("LABEL"))
        dr.sendMessage(BytesPacket("Test3".toByteArray(), HostID.Local, rh))
        sde(s.queue.pop())
        assertSame(nep, e)
        assertEquals("Test3", String((d as BytesPacket).data))
        assertTrue(s.queue.size == 0)
        nep.close()

        // Отправка пакета с меткой в конечную точку, у которой AcceptTags не установлен
        nep = NetworkEndPoint(dr, s, rh, IPAddressDirectory(), 1234, null)
        dr.sendMessage(BytesPacket("Test1".toByteArray(), HostID.Local, rh).apply { tags = arrayOf("LABEL") })
        sde(s.queue.pop())
        assertSame(nep, e)
        assertEquals("Test1", String((d as BytesPacket).data))
        assertTrue(s.queue.size == 0)
        nep.close()

        // Отправка пакета с меткой в конечную точку, у которой AcceptTags установлен но пуст
        nep = NetworkEndPoint(dr, s, rh, IPAddressDirectory(), 1234, arrayOf(""))
        dr.sendMessage(BytesPacket("Test2".toByteArray(), HostID.Local, rh).apply { tags = arrayOf("LABEL") })
        sde(s.queue.pop())
        assertSame(nep, e)
        assertEquals("Test2", String((d as BytesPacket).data))
        assertTrue(s.queue.size == 0)
        nep.close()

        // Отправка пакета с меткой в конечную точку, у которой AcceptTags установлен 
        nep = NetworkEndPoint(dr, s, rh, IPAddressDirectory(), 1234, arrayOf("LABEL"))
        dr.sendMessage(BytesPacket("Test3".toByteArray(), HostID.Local, rh).apply { tags = arrayOf("LABEL") })
        sde(s.queue.pop())
        assertSame(nep, e)
        assertEquals("Test3", String((d as BytesPacket).data))
        assertTrue(s.queue.size == 0)
        nep.close()

        // Отправка пакета с меткой в конечную точку, у которой AcceptTags установлен
        // Метки у пакета и конечной точки разные
        nep = NetworkEndPoint(dr, s, rh, IPAddressDirectory(), 1234, arrayOf("LABEL1"))
        dr.sendMessage(BytesPacket("Test4".toByteArray(), HostID.Local, rh).apply { tags = arrayOf("LABEL") })
        assertTrue(s.queue.size == 0)
        nep.close()
    }
}