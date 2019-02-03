package utils

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.utils.*

class AckMonitorTest
{
    @Test
    fun Test() = runBlocking {
        val (tk, j) = register<BinaryReader>()

        val br = BinaryReader(byteArrayOf())

        launch { received(tk, br) }

        val z = j.await()

        assertSame(br, z)
    }
}
