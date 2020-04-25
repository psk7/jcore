package pvt.psk.jcore.utils

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class BinaryReaderTest {

    @Test
    fun readByte() {
        var r = BinaryReader(byteArrayOf(0xff.toByte(), 1.toByte(), 2.toByte()))

        assertEquals(255, r.readByte())
        assertEquals(1, r.readByte())
        assertEquals(2, r.readByte())
    }
}