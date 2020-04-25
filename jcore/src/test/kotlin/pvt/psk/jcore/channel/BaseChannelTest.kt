@file:Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")

package pvt.psk.jcore.channel

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*
import utils.*
import java.io.*

class BaseChannelTest : JcoreKoinTest() {

    @Test
    fun getChannelAcceptTag() {
        val bc = BaseChannel("Channel", 0U, {})
        val c = bc.getChannel(null, "TAG1")

        assertEquals(1, bc.acceptTags.size)
        assertEquals(PacketTag("TAG1"), bc.acceptTags[0])
    }

    @Test
    fun receiveStreamPacketWithNoConsumers() {
        val hf = HostID.new("From");
        val bc = BaseChannel("Channel", 0U, { })

        val sp = StreamPacket().apply { sourceStream = ByteArrayInputStream(ByteArray(40000)) }

        var r = bc.getChannel({ e, p -> })

        bc.receivedFromRelay(ChannelEnvelope(hf, HostID.Local, sp));

        runBlocking { sp.completeStreamAsync() }
    }
}