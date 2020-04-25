package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

class JobTest {

    @Test
    fun multipleCompletion() {
        val j = SupervisorJob()

        var cnt = 0

        j.invokeOnCompletion { cnt++; println("!") }
        j.invokeOnCompletion { cnt++; println("!") }
        j.invokeOnCompletion { cnt++; println("!") }

        runBlocking { j.cancelAndJoin() }

        assertEquals(3, cnt)
    }
}