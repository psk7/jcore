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

    override fun ProcessHostInfoCommand(Command: HostInfoCommand) {
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

        val pp = TestPeerProtocol(hid, "TestDomain", r.getChannel(), l)

        assertEquals(hid, pp.selfHostID)
    }

    @Test
    fun discovery() {
        val l = TestLogger("P1")
        val r = Router()

        val hid = HostID(UUID.randomUUID(), "Host")

        val pp = TestPeerProtocol(hid, "TestDomain", r.getChannel(), l)

        val dc = r.getChannel()

        val lst = mutableListOf<Message>()

        dc.received += { (_, p) -> lst.add(p) }

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

        val pp = TestPeerProtocol(hid, "TestDomain", r.getChannel(), l)

        val dc = r.getChannel()

        val lst = mutableListOf<Message>()

        dc.received += { (_, p) -> lst.add(p) }

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
        var fromip = InetSocketAddress(Inet6Address.getLoopbackAddress(), 1111)

        var pp = TestPeerProtocol(hid, "TestDomain", r.getChannel(), l)

        val dc = r.getChannel()

        val lst = mutableListOf<Message>()

        dc.received += { (_, p) -> lst.add(p) }

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

}