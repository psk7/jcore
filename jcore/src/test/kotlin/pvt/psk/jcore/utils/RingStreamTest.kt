package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.*
import java.nio.*
import kotlin.random.*

class RingStreamTest {

    @ExperimentalStdlibApi
    @Test
    fun largeWrite() {

        val rs = RingStream(4)

        val str = "TEST1234";

        val b = Charsets.US_ASCII.encode(str).array()

        GlobalScope.launch {
            rs.write(b, 0, b.size)
            rs.close()
        }

        val ms = ByteArrayOutputStream()
        var readed = 0

        readed = rs.read(b, 0, b.size)
        while (readed != -1) {
            ms.write(b, 0, readed)
            readed = rs.read(b, 0, b.size)
        }

        assertEquals(str, Charsets.US_ASCII.decode(ByteBuffer.wrap(ms.toByteArray())).toString())
    }

    @Test
    fun writeAndClose() {
        val rs = RingStream()
        val outs = rs.getOutputStream()
        val ins = rs.getInputStream()

        val r = Random(0)
        val b = r.nextBytes(10000)

        outs.write(b, 0, b.size)
        outs.close()

        val bb = ByteArray(20000)
        val rdd = ins.read(bb, 0, bb.size)

        val rr = bb.take(rdd).toByteArray()

        assertTrue(rr.contentEquals(b))
    }

    @Test
    fun readEmpty() {
        val rs = RingStream().getInputStream()

        rs.close()

        val b = rs.read()

        assertEquals(-1, b)
    }
}