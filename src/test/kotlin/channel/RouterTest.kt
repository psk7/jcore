package channel

import org.junit.jupiter.api.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import java.util.*
import org.junit.jupiter.api.Assertions.*

class RouterTest
{
    class TestMessage(val Value: String, ToHost: HostID) : Message(HostID.Local, ToHost)

    fun compare(List: MutableList<TestMessage>, vararg inds: Int)
    {
        assert(List.count() == inds.count())

        inds.forEachIndexed { index, i -> assertEquals(List[index].Value, "Test" + i.toString()) }
    }

    @Test
    fun Channels()
    {
        val h1 = HostID(UUID.randomUUID(), "Host1")
        val h2 = HostID(UUID.randomUUID(), "Host2")
        val h3 = HostID(UUID.randomUUID(), "Host3")

        val r = Router()

        val ch1 = r.getChannel(true, h1)
        val ch2 = r.getChannel(acceptHost = h2)
        val ch3 = r.getChannel(acceptHost = h3)
        val ch4 = r.getChannel(true)
        val ch5 = r.getChannel()

        val l1 = mutableListOf<TestMessage>()
        val l2 = mutableListOf<TestMessage>()
        val l3 = mutableListOf<TestMessage>()
        val l4 = mutableListOf<TestMessage>()
        val l5 = mutableListOf<TestMessage>()

        ch1.received += { (_, p) -> l1.add(p as TestMessage) }
        ch2.received += { (_, p) -> l2.add(p as TestMessage) }
        ch3.received += { (_, p) -> l3.add(p as TestMessage) }
        ch4.received += { (_, p) -> l4.add(p as TestMessage) }
        ch5.received += { (_, p) -> l5.add(p as TestMessage) }

        ch1.sendMessage(TestMessage("Test1", HostID.All))
        ch1.sendMessage(TestMessage("Test2", h1))
        ch1.sendMessage(TestMessage("Test3", h2))
        ch2.sendMessage(TestMessage("Test4", h2))
        ch1.sendMessage(TestMessage("Test5", h3))
        ch5.sendMessage(TestMessage("Test6", HostID.All))
        ch4.sendMessage(TestMessage("Test7", HostID.All))
        ch3.sendMessage(TestMessage("Test8", h1))

        compare(l1, 1, 2, 6, 7, 8)
        compare(l2, 1, 3, 6, 7)
        compare(l3, 1, 5, 6, 7)
        compare(l4, 1, 2, 3, 4, 5, 6, 7, 8)
        compare(l5, 1, 2, 3, 4, 5, 7, 8)
    }

}