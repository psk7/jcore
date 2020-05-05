@file:Suppress("UNUSED_VARIABLE", "TestFunctionName")

package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class AckMonitorTest {
    @Test
    fun Test() = runBlocking {
        val tk = registerAckToken()

        val br = BinaryReader(byteArrayOf())

        launch { tk.received(br) }

        val z = tk.await<BinaryReader>()

        assertSame(br, z)
    }

    @Test
    fun timeOut() {
        val tk = registerAckToken(10)

        try {
            runBlocking { tk.await<BinaryReader>() }
        }
        catch (e: Exception) {
        }
    }
}
