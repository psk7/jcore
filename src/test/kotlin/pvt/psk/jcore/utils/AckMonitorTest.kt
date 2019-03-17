@file:Suppress("UNUSED_VARIABLE", "TestFunctionName")

package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@ExperimentalCoroutinesApi
class AckMonitorTest {
    @Test
    fun Test() = runBlocking {
        val (tk, j) = register<BinaryReader>()

        val br = BinaryReader(byteArrayOf())

        launch { tk.received(br) }

        val z = j.await()

        assertSame(br, z)
    }

    @Test
    fun timeOut() {
        val (tk, j) = register<BinaryReader>(10)

        try {
            j.wait()
        } catch (e: Exception) {
        }
    }

    @Test
    fun cancelSafeAwait() {
        val (tk, j) = register<BinaryReader>(10)

        runBlocking {
            assertNull(j.cancelSafeAwait())
        }
    }
}
