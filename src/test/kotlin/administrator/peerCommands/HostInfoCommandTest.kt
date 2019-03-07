package administrator.peerCommands

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import java.util.*

class HostInfoCommandTest {
    @Test
    fun complete() {
        val fh = HostID(UUID.randomUUID(), "From")
        val ft = HostID(UUID.randomUUID(), "To")

        val j1 = CompletableDeferred<Unit>()
        val j2 = CompletableDeferred<Unit>()
        val j3 = CompletableDeferred<Unit>()

        val h = HostInfoCommand(0, fh, arrayOf(), ft)

        h.addTask(j1)
        h.addTask(j2)
        h.addTask(j3)

        val fj = h.complete

        assertFalse(fj.isCompleted)

        j1.complete(Unit)
        assertFalse(fj.isCompleted)

        j2.complete(Unit)
        assertFalse(fj.isCompleted)

        j3.complete(Unit)
        assertFalse(fj.isCompleted)

        h.release()

        assertTrue(fj.isCompleted)
    }
}