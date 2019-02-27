package administrator

import channel.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.HostID
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*
import java.time.*
import java.util.*

private class TestLogger(val prefix: String) : Logger() {
    override fun writeLog(TimeStamp: LocalDateTime, importance: LogImportance, logCat: String, message: String) {
        println(message)
    }
}

private class TestPeerProtocol(selfHostID: HostID, domain: String, controlChannel: IChannel, logger: Logger?) :
    PeerProtocol(selfHostID, domain, controlChannel, logger) {
    override fun create(Reader: BinaryReader): PeerCommand? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun serialize(Command: PeerCommand, Writer: BinaryWriter) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun processHostInfoCommand(Command: HostInfoCommand) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createPollCommand(): PollCommand = TestPollCommand(selfHostID)
}

private class TestHostInfoCommand(SequenceID: Int, FromHost: HostID, endPoints: Array<EndPointInfo>, ToHost: HostID,
                                  vararg payload: Array<Any>) :
    HostInfoCommand(SequenceID, FromHost, endPoints, ToHost, *payload) {
}

private class TestPollCommand(ToHost: HostID) : PollCommand(HostID.Local, ToHost) {
    override fun createHostInfoCommand(SeqID: Int, FromHost: HostID, ToHost: HostID): HostInfoCommand {
        return TestHostInfoCommand(SeqID, FromHost, arrayOf(), ToHost)
    }
}

class PeerProtocolTest {

    @Test
    fun create() {
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")

        val pp = TestPeerProtocol(hid, "TestDomain", r, l)

        assertEquals(hid, pp.selfHostID)
    }

    @Test
    fun discovery() {
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")

        val pp = TestPeerProtocol(hid, "TestDomain", r, l)

        val lst = mutableListOf<Message>()

        val dc = r.getChannel({ _, p -> lst.add(p) })

        pp.discovery()

        assertEquals(1, lst.size)

        val dp = lst[0] as DiscoveryCommand

        // Discovery отсылается только в сеть
        assertEquals(HostID.Network, dp.ToHost)
        assertEquals(hid, dp.FromHost)
    }

    @Test
    fun leave() {
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")

        val pp = TestPeerProtocol(hid, "TestDomain", r, l)

        val lst = mutableListOf<Message>()

        val dc = r.getChannel({ _, p -> lst.add(p) })

        pp.leave()

        assertEquals(1, lst.size)

        val dp = lst[0] as LeaveCommand

        // Leave отсылается только в сеть
        assertEquals(HostID.Network, dp.ToHost)
        assertEquals(hid, dp.FromHost)
    }

    @Test
    fun onDiscovery() {
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")
        val fromhost = HostID(UUID.randomUUID(), "FromHost")
        val fromip = InetSocketAddress(Inet6Address.getLoopbackAddress(), 1111)

        val pp = TestPeerProtocol(hid, "TestDomain", r, l)

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
        var r = Router()
        var remote = HostID(UUID.randomUUID(), "Remote")
        var self = HostID(UUID.randomUUID(), "Self")

        var pp = TestPeerProtocol(self, "TestDomain", r, null)

        val D = { f: HostID, t: HostID ->
            val r = pp.adjustTargetHost(f, t)

            if (r.first)
                DiscoveryCommand(f, r.second)
            else
                null
        }

        // remote отсылал пакет с отправителя Local -> ALL. Формируется remote
        var c = D(HostID.Local, HostID.All)
        assertEquals(HostID.Local, c!!.FromHost)
        assertEquals(HostID.Local, c.ToHost)

        // remote отсылал пакет со своего HostID -> ALL. Формируется remote
        c = D(remote, HostID.All)
        assertEquals(remote, c!!.FromHost)
        assertEquals(HostID.Local, c.ToHost)

        assertNull(D(HostID.Local, HostID.Network))
        assertNull(D(remote, HostID.Network))

        // remote отсылал пакет с отправителя Local -> self. Формируется remote
        c = D(HostID.Local, self)
        assertEquals(HostID.Local, c!!.FromHost)
        assertEquals(HostID.Local, c.ToHost)

        // remote отсылал пакет со своего HostID -> self. Формируется remote
        // приемник Local
        c = D(remote, self)
        assertEquals(remote, c!!.FromHost)
        assertEquals(HostID.Local, c.ToHost)
    }
}