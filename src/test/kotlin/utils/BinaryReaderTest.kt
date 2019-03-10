package utils

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.utils.*

class BinaryReaderTest {

    @Test
    fun readByte() {
        val r = BinaryReader(byteArrayOf(0xff.toByte(), 1.toByte(), 2.toByte()))

        assertEquals(255.toByte(), r.readByte())
        assertEquals(1.toByte(), r.readByte())
        assertEquals(2.toByte(), r.readByte())
    }
}