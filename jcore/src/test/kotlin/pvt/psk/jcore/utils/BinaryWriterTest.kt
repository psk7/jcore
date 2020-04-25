package pvt.psk.jcore.utils

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

internal class BinaryWriterTest {

    @Test
    fun write7BitEncodedString() {
        val wr = BinaryWriter()
        wr.write7BitEncodedInt(1320334537)

        val rd = BinaryReader(wr.toArray())
        val v = rd.read7BitEncodedInt()

        assertEquals(1320334537, v)
    }
}