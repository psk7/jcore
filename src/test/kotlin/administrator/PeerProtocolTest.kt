package administrator

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.HostID
import pvt.psk.jcore.logger.*
import java.util.*

private class TestLogger(val prefix: String) : Logger() {
}

private class TestPeerProtocol(selfHostID: HostID, domain: String, controlChannel: IChannel, logger: Logger?) :
    PeerProtocol(selfHostID, domain, controlChannel, logger) {

    override fun ProcessHostInfoCommand(Command: HostInfoCommand) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createPollCommand(): PollCommand {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}