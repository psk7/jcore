package pvt.psk.jcore.relay

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.utils.*

class PacketTagTest {

    @Test
    fun fixed() {
        val pt = PacketTag("INPU")

        assertEquals(1431326281, pt.id)

        val pt2 = PacketTag("STRP")

        assertEquals(1347572819, pt2.id)
    }

    @Test
    fun deserialize() {
        val rd = BinaryReader(byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte()))

        val pt = PacketTag(rd)

        assertEquals(-1, pt.id)
    }
}