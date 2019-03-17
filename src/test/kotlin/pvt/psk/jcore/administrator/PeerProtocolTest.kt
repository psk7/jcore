@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE", "NAME_SHADOWING", "UNCHECKED_CAST")

package pvt.psk.jcore.administrator

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.channel.commands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.util.*
import java.util.concurrent.*

@ExperimentalCoroutinesApi
private class TestPeerProtocol(selfHostID: HostID, controlChannel: IChannel, logger: Logger?) :
        PeerProtocol(selfHostID, controlChannel, logger) {

    override fun createPollCommand(): PollCommand = TestPollCommand(selfHostID)
}

private class TestHostInfoCommand(SequenceID: Int, FromHost: HostID, endPoints: Array<EndPointInfo>, ToHost: HostID,
                                  vararg payload: Array<Any>) :
        HostInfoCommand(SequenceID, FromHost, endPoints, ToHost, *payload) {
}

@ExperimentalCoroutinesApi
private class TestPollCommand(ToHost: HostID) : PollCommand() {
    override fun createHostInfoCommand(SeqID: Int, FromHost: HostID, ToHost: HostID): HostInfoCommand {
        return TestHostInfoCommand(SeqID, FromHost, arrayOf(), ToHost)
    }
}

private class MsgQueue() : LinkedBlockingQueue<Message>() {

    fun <T> tryGetMessage(): T where T : Message {

        assertFalse(isEmpty())

        return take() as T
    }
}

@ExperimentalCoroutinesApi
class PeerProtocolTest {

    @Test
    fun create() {
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")

        val pp = TestPeerProtocol(hid, r, l)

        assertEquals(hid, pp.selfHostID)
    }

    @Test
    fun discovery() {
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")

        val pp = TestPeerProtocol(hid, r, l)

        val lst = mutableListOf<Message>()

        val dc = r.getChannel({ _, p -> lst.add(p) })

        pp.discovery()

        assertEquals(1, lst.size)

        val dp = lst[0] as DiscoveryCommand

        // Discovery отсылается только в сеть
        assertEquals(HostID.Network, dp.toHost)
        assertEquals(hid, dp.fromHost)
    }

    @Test
    fun onDiscovery() {
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")
        val fromhost = HostID(UUID.randomUUID(), "FromHost")
        val fromip = InetSocketAddress(Inet6Address.getLoopbackAddress(), 1111)

        val pp = TestPeerProtocol(hid, r, l)

        val lst = mutableListOf<Message>()

        val dc = r.getChannel({ _, p -> lst.add(p) })

        dc.sendMessage(DiscoveryCommand(fromhost, HostID.Local))

        assertEquals(3, lst.size)

        assert(lst[0] is TestPollCommand)
        assert(lst[1] is TestHostInfoCommand)
        assert(lst[2] is DiscoveryCommand)

        // Повторный прием от того же хоста
        dc.sendMessage(DiscoveryCommand(fromhost, HostID.Local))

        // Протокол будет знать отправителя только после прием HostInfo
        // поэтому при повторном приеме все сообщения повторятся

        assertEquals(6, lst.size)
        assert(lst[3] is TestPollCommand)
        assert(lst[4] is TestHostInfoCommand)
        assert(lst[5] is DiscoveryCommand)
    }

    @Test
    fun adjustTargetHost() {
        val r = Router()
        val remote = HostID(UUID.randomUUID(), "Remote")
        val self = HostID(UUID.randomUUID(), "Self")

        val pp = TestPeerProtocol(self, r, null)

        val D = { f: HostID, t: HostID ->
            val r = pp.adjustTargetHost(f, t)

            if (r.first)
                DiscoveryCommand(f, r.second)
            else
                null
        }

        // remote отсылал пакет с отправителя Local -> ALL. Формируется remote
        var c = D(HostID.Local, HostID.All)
        assertEquals(HostID.Local, c!!.fromHost)
        assertEquals(HostID.Local, c.toHost)

        // remote отсылал пакет со своего HostID -> ALL. Формируется remote
        c = D(remote, HostID.All)
        assertEquals(remote, c!!.fromHost)
        assertEquals(HostID.Local, c.toHost)

        assertNull(D(HostID.Local, HostID.Network))
        assertNull(D(remote, HostID.Network))

        // remote отсылал пакет с отправителя Local -> self. Формируется remote
        c = D(HostID.Local, self)
        assertEquals(HostID.Local, c!!.fromHost)
        assertEquals(HostID.Local, c.toHost)

        // remote отсылал пакет со своего HostID -> self. Формируется remote
        // приемник Local
        c = D(remote, self)
        assertEquals(remote, c!!.fromHost)
        assertEquals(HostID.Local, c.toHost)
    }

    @Test
    fun onHostInfo() {
        var seq = 0
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")
        val rh1 = HostID(UUID.randomUUID(), "Remote1")
        val rh2 = HostID(UUID.randomUUID(), "Remote2")
        val rh3 = HostID(UUID.randomUUID(), "Remote3")

        val pp = TestPeerProtocol(hid, r, l)

        val lst = MsgQueue()

        val bc1 = BaseChannelTest.BC("Chan1", Router(), Router(), null, CancellationToken.None)
        val bc2 = BaseChannelTest.BC("Chan2", Router(), Router(), null, CancellationToken.None)
        val bc3 = BaseChannelTest.BC("Chan3", Router(), Router(), null, CancellationToken.None)

        // ReSharper disable once UnusedVariable
        val dc = r.getChannel({ _, p ->
                                  when (p) {
                                      is PollCommand -> {
                                          p.registerChannel("Chan1", bc1)
                                          p.registerChannel("Chan2", bc2)
                                          p.registerChannel("Chan3", bc3)
                                      }

                                      is HostInfoCommand -> {
                                      }

                                      else -> lst.add(p)
                                  }
                              })

        val ep1 = EndPointInfo(rh1, "Chan1", false)
        val ep2 = EndPointInfo(rh2, "Chan2", false)
        val ep3 = EndPointInfo(rh2, "Chan1", false)
        val ep4 = EndPointInfo(rh3, "Chan1", false)
        val ep5 = EndPointInfo(rh1, "Chan3", false)

        // Добавлен rh1 в канал Chan1
        dc.sendMessage(HostInfoCommand(++seq, rh1, arrayOf(ep1), HostID.Local))

        var cmd = lst.tryGetMessage<NewHostInChannelCommand>()
        assertEquals(rh1, cmd.endPointInfo.target)
        assertEquals("Chan1", cmd.endPointInfo.channelName)

        // Добавлен rh2 в канал Chan2
        dc.sendMessage(HostInfoCommand(++seq, rh2, arrayOf(ep2), HostID.Local))

        cmd = lst.tryGetMessage()
        assertEquals(rh2, cmd.endPointInfo.target)
        assertEquals("Chan2", cmd.endPointInfo.channelName)

        // Повторим предыдущую команду
        dc.sendMessage(HostInfoCommand(++seq, rh2, arrayOf(ep2), HostID.Local))

        // Должно прийти обновление канала
        var cmdu = lst.tryGetMessage<UpdateHostInChannelCommand>()
        assertEquals(rh2, cmdu.endPointInfo.target)
        assertEquals("Chan2", cmdu.endPointInfo.channelName)

        // Добавлен rh2 в канал Chan1
        dc.sendMessage(HostInfoCommand(++seq, rh2, arrayOf(ep2, ep3), HostID.Local))

        cmdu = lst.tryGetMessage()
        assertEquals(rh2, cmdu.endPointInfo.target)
        assertEquals("Chan2", cmdu.endPointInfo.channelName)
        cmd = lst.tryGetMessage()
        assertEquals(rh2, cmd.endPointInfo.target)
        assertEquals("Chan1", cmd.endPointInfo.channelName)

        // Добавлен rh3 в канал Chan1
        dc.sendMessage(HostInfoCommand(++seq, rh3, arrayOf(ep4), HostID.Local))

        cmd = lst.tryGetMessage()
        assertEquals(rh3, cmd.endPointInfo.target)
        assertEquals("Chan1", cmd.endPointInfo.channelName)

        // rh2 вышел из всех каналов
        dc.sendMessage(HostInfoCommand(++seq, rh2, arrayOf(), HostID.Local))

        var cmdl = lst.tryGetMessage<HostLeaveChannelCommand>()
        assertEquals(rh2, cmdl.leavedHost)
        assertEquals("Chan2", cmdl.channel)
        cmdl = lst.tryGetMessage()
        assertEquals(rh2, cmdl.leavedHost)
        assertEquals("Chan1", cmdl.channel)

        // Повторим предыдущую команду
        dc.sendMessage(HostInfoCommand(++seq, rh2, arrayOf(), HostID.Local))

        // Ничего не должно произойти
        assertTrue(lst.isEmpty())

        // rh1 вышел из Chan1 и зашел в Chan3
        dc.sendMessage(HostInfoCommand(++seq, rh1, arrayOf(ep5), HostID.Local))
        cmd = lst.tryGetMessage()
        assertEquals(rh1, cmd.endPointInfo.target)
        assertEquals("Chan3", cmd.endPointInfo.channelName)
        cmdl = lst.tryGetMessage()
        assertEquals(rh1, cmdl.leavedHost)
        assertEquals("Chan1", cmdl.channel)

        // rh1 вышел из всех каналов
        dc.sendMessage(HostInfoCommand(++seq, rh1, arrayOf(), HostID.Local))
        cmdl = lst.tryGetMessage()
        assertEquals(rh1, cmdl.leavedHost)
        assertEquals("Chan3", cmdl.channel)

        assertTrue(lst.isEmpty())
    }
}