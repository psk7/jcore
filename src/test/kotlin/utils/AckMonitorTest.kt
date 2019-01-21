package utils

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.utils.AckMonitor.Companion.Register
import pvt.psk.jcore.utils.AckMonitor.Companion.Received
import pvt.psk.jcore.utils.*

class AckMonitorTest
{
    @Test
    fun Test() = runBlocking {
        val (tk, j) = Register()

        val br = BinaryReader()

        GlobalScope.launch { Received(tk, br) }

        val z = j.await()

        assertSame(br, z)
    }
}
