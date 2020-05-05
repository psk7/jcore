package pvt.psk.jcore.utils

import io.mockk.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.relay.*
import utils.*

class RevisionEnsuratorTest : JcoreKoinTest() {

    class Reply(override var ack: AckToken, override var accepted: Boolean) : RevisionEnsurator.IReply

    @Test
    @Timeout(2)
    fun receive() {
        var ee: RevisionEnsurator<RelayID>? = null
        val e by lazy { ee!! }

        val m = mockk<(RelayID, AckToken) -> Boolean>()

        every { m.invoke(any(), any()) } answers {
            val ack = it.invocation.args.elementAt(1) as AckToken

            e.replyReceived(Reply(ack, true))

            true
        }

        ee = RevisionEnsurator(m, 60.seconds)

        val rid = RelayID(1)

        assertTrue(runBlocking { e.ensure(rid) })
        assertTrue(runBlocking { e.ensure(rid) }) // Повторный вызов. Должен вернуть true без повторной отправки

        verify(exactly = 1) { m.invoke(any(), any()) }

        e.bumpRevision()

        assertTrue(runBlocking { e.ensure(rid) }) // Должен быть вызов

        verify(exactly = 2) { m.invoke(any(), any()) }

        unmockkAll()
    }

    @Test
    @Timeout(2)
    fun receiveList() {
        var ee: RevisionEnsurator<RelayID>? = null
        val e by lazy { ee!! }

        val m = mockk<(RelayID, AckToken) -> Boolean>()

        every { m.invoke(any(), any()) } answers {
            val ack = it.invocation.args.elementAt(1) as AckToken

            e.replyReceived(Reply(ack, true))

            true
        }

        ee = RevisionEnsurator(m, 60.seconds)

        val l = arrayOf(RelayID(1))

        assertEquals(0, runBlocking { e.ensure(l).size })
        assertEquals(0, runBlocking { e.ensure(l).size }) // Повторный вызов. Должен вернуть true без повторной отправки

        verify(exactly = 1) { m.invoke(any(), any()) }

        e.bumpRevision()

        assertEquals(0, runBlocking { e.ensure(l).size }) // Должен быть вызов

        verify(exactly = 2) { m.invoke(any(), any()) }

        unmockkAll()
    }

    @Test
    fun timeout() {
        val m = mockk<(RelayID, AckToken) -> Boolean>()

        every { m.invoke(any(), any()) } returns true

        val rid = RelayID(1)

        val e = RevisionEnsurator(m, 1.milliseconds)

        assertFalse(runBlocking { e.ensure(rid) })

        verify(exactly = 1) { m.invoke(any(), any()) }

        unmockkAll()
    }

    @Test
    fun listTimeout() {
        val m = mockk<(RelayID, AckToken) -> Boolean>()

        every { m.invoke(any(), any()) } returns true

        val l = arrayOf(RelayID(1))

        val e = RevisionEnsurator(m, 1.milliseconds)

        assertTrue(l.contentEquals(runBlocking { e.ensure(l) }.toTypedArray()))

        verify(exactly = 1) { m.invoke(any(), any()) }

        unmockkAll()
    }
}