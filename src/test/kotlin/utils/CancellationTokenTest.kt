package utils

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.utils.*
import java.time.*

class CancellationTokenTest {

    @Test
    fun testCancel() {
        val cts = CancellationTokenSource()
        val ct = cts.token

        assertFalse(ct.isCancellationRequested)

        cts.cancel()

        assertTrue(ct.isCancellationRequested)
    }

    @Test
    fun testTimeout1() {
        val cts = CancellationTokenSource(Duration.ofMillis(10))
        val ct = cts.token

        while (!ct.isCancellationRequested) {
            runBlocking { delay(1) }
        }
    }

    @Test
    fun testTimeout2() {
        val cts = CancellationTokenSource()
        val ct = cts.token

        cts.cancelAfter(Duration.ofMillis(20))

        while (!ct.isCancellationRequested) {
            runBlocking { delay(1) }
        }
    }

    @Test
    fun register1() {
        val ccts = CancellationTokenSource()
        val cct = ccts.token

        val cts = CancellationTokenSource(Duration.ofMillis(10))
        val ct = cts.token

        ct.register { ccts.cancel() }

        while (!cct.isCancellationRequested) {
            runBlocking { delay(1) }
        }
    }

    @Test
    fun register2() {

        val ccts = CancellationTokenSource()
        val cct = ccts.token

        val cts = CancellationTokenSource()
        val ct = cts.token

        ct.register { ccts.cancel() }.close()

        cts.cancel()

        assertFalse(ccts.isCancellationRequested)
    }

    @Test
    fun regOnNone() {

        // Попытка регистрации отмены на CancellationToken.None должна игнорироваться
        val ct = CancellationToken.None

        var r = ct.register {
            throw Exception()
        }
    }
}