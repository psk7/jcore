package pvt.psk.jcore.relay

import io.mockk.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*
import utils.*
import kotlin.coroutines.*

class BaseRelayTest : JcoreKoinTest() {

    @Test
    fun receivedOwnRelayInfo() {
        val rid = RelayID(1)
        val orid = RelayID(2)
        val hid1 = HostID(1)
        val hid2 = HostID(2)

        val tr = object : BaseRelay(rid) {
            override val coroutineContext: CoroutineContext
                get() = Dispatchers.Unconfined + job

            fun addAdjacentRelayOverride(id: RelayID, sink: RelayMessageToBoolean) = addAdjacentRelay(id, sink)
            fun receivedOverride(message: RelayMessage) = received(message)
        }

        val m = mockk<(RelayMessage) -> Boolean>()

        every { m.invoke(any()) } returns true

        tr.addAdjacentHost(hid1, {})
        tr.addAdjacentRelayOverride(orid, m)

        // Сообщение от orid, но с данными самого rid (ошибочная ситуация)
        val ri = RelayInfo(rid, arrayOf(hid1), arrayOf(), AckToken.empty, 0)
        val rm = RelayMessage(orid, rid, ri, 1U)

        tr.receivedOverride(rm)

        // Не должно быть попытки ретрансляции сообщения по адресу orid
        tr.send(
            RelayEnvelope(HostEndpointID(hid2, 10U), arrayOf(HostEndpointID(hid1, 10U)), BytesPacket(byteArrayOf(0))))

        assertEquals(1, tr.adjacentHosts.size)
        assertEquals(1, tr.adjacentRelays.size)

        verify(exactly = 1) { m.invoke(any()) }

        unmockkAll()
    }

    @Test
    fun ensureAdjacentRelayHasLatestConfiguration() {
        val rid1 = RelayID(1)
        val rid2 = RelayID(2)

        val tr = object : BaseRelay(rid1) {
            override val coroutineContext: CoroutineContext
                get() = Dispatchers.Unconfined + SupervisorJob()

            fun addAdjacentRelayOverride(id: RelayID, sink: RelayMessageToBoolean) = super.addAdjacentRelay(id, sink)

            suspend fun ensureAdjacentRelayHasLatestConfigurationOverride(relay: RelayID) =
                confirm.ensure(relay)

            fun addAdjacentHostOverride(id: HostID, sink: (RelayEnvelope) -> Unit, onRouteChanged: (() -> Unit)?) =
                addAdjacentHost(id, sink, onRouteChanged)

            val confirm
                get() = adjacentRelayReceiveRelayInfoConfirm
        }

        val m = mockk<(RelayMessage) -> Boolean>()

        every { m.invoke(any()) } answers {
            val rm = it.invocation.args.first() as RelayMessage
            val ri = rm.payload as RelayInfo

            ri.ack.received(RelayInfoReply(rid1, ri.ack, false))

            true
        }

        tr.addAdjacentRelayOverride(rid2, m)

        assertTrue(runBlocking { tr.ensureAdjacentRelayHasLatestConfigurationOverride(rid2) })

        // Повторный вызов. Должен быть проигнорирован.
        assertTrue(runBlocking { tr.ensureAdjacentRelayHasLatestConfigurationOverride(rid2) })

        val h = HostID(3)
        tr.addAdjacentHost(h, {}, null)

        // Повторный вызов. Должен быть проигнорирован.
        assertTrue(runBlocking { tr.ensureAdjacentRelayHasLatestConfigurationOverride(rid2) })

        unmockkAll()
    }

    @Test
    fun receiveRelayInfoReplyWithNonAccept() {
        val rid1 = RelayID(1)
        val rid2 = RelayID(2)

        val tr = object : BaseRelay(rid1) {
            override val coroutineContext = Dispatchers.Unconfined + SupervisorJob()

            fun addAdjacentRelayOverride(id: RelayID, sink: RelayMessageToBoolean) = super.addAdjacentRelay(id, sink)

            suspend fun ensureAdjacentRelayHasLatestConfigurationOverride(relay: RelayID) =
                confirm.ensure(relay)

            val confirm
                get() = adjacentRelayReceiveRelayInfoConfirm
        }

        val m = mockk<(RelayMessage) -> Boolean>()

        every { m.invoke(any()) } answers {
            val rm = it.invocation.args.first() as RelayMessage
            val ri = rm.payload as RelayInfo

            ri.ack.received(RelayInfoReply(rid1, ri.ack, false))

            true
        }

        tr.addAdjacentRelayOverride(rid2, m)

        assertTrue(runBlocking { tr.ensureAdjacentRelayHasLatestConfigurationOverride(rid2) })

        unmockkAll()
    }

    @Test
    fun receiveRelayInfoReplyWithAccept() {
        val rid1 = RelayID(1)
        val rid2 = RelayID(2)

        val tr = object : BaseRelay(rid1) {
            fun addAdjacentRelayOverride(id: RelayID, sink: RelayMessageToBoolean) = super.addAdjacentRelay(id, sink)

            suspend fun ensureAdjacentRelayHasLatestConfigurationOverride(relay: RelayID) =
                confirm.ensure(relay)

            val confirm
                get() = adjacentRelayReceiveRelayInfoConfirm
        }

        val m = mockk<(RelayMessage) -> Boolean>()

        every { m.invoke(any()) } answers {
            val rm = it.invocation.args.first() as RelayMessage
            val ri = rm.payload as RelayInfo

            ri.ack.received(RelayInfoReply(rid1, ri.ack, true))

            true
        }

        tr.addAdjacentRelayOverride(rid2, m)

        assertTrue(runBlocking { tr.ensureAdjacentRelayHasLatestConfigurationOverride(rid2) })

        unmockkAll()
    }

    @Test
    fun addAdjacentHost() {
        val rid1 = RelayID(1)
        val hid1 = HostID(1)

        val r = object : BaseRelay(rid1) {
            fun addAdjacentHostOverride(id: HostID, sink: (RelayEnvelope) -> Unit, onRouteChanged: (() -> Unit)?) =
                addAdjacentHost(id, sink, onRouteChanged)

            val relayConfigurationRevisionOverride
                get() = confirm.revision

            val confirm
                get() = adjacentRelayReceiveRelayInfoConfirm
        }

        r.addAdjacentHostOverride(hid1, {}, null)
        r.addAdjacentHostOverride(hid1, {}, null)
        r.addAdjacentHostOverride(hid1, {}, null)

        assertEquals(2, r.relayConfigurationRevisionOverride) // Счетчик увеличился 1 раз. (Начальное значение 1)
    }

    @Test
    fun addAdjacentRelay() {
        val rid1 = RelayID(1)

        val tr = object : BaseRelay(rid1) {
            fun addAdjacentRelayOverride(id: RelayID, sink: RelayMessageToBoolean) = super.addAdjacentRelay(id, sink)

            val relayConfigurationRevisionOverride
                get() = confirm.revision

            val confirm
                get() = adjacentRelayReceiveRelayInfoConfirm
        }

        tr.addAdjacentRelayOverride(rid1) { true }
        tr.addAdjacentRelayOverride(rid1) { true }
        tr.addAdjacentRelayOverride(rid1) { true }

        assertEquals(2, tr.relayConfigurationRevisionOverride) // Счетчик увеличился 1 раз. (Начальное значение 1)
    }

    @Test
    fun addAdjacentRelay2() {
        val rid1 = RelayID(1)
        val rid2 = RelayID(2)

        val tr = object : BaseRelay(rid1) {
            override val coroutineContext: CoroutineContext
                get() = Dispatchers.Unconfined + job

            fun addAdjacentRelayOverride(id: RelayID, sink: RelayMessageToBoolean) = super.addAdjacentRelay(id, sink)
        }

        val m = mockk<(RelayMessage) -> Boolean>()

        every { m.invoke(ofType(RelayMessage::class)) } answers {
            val rm = it.invocation.args.first() as RelayMessage
            val ri = rm.payload as RelayInfo

            // Имитация прихода положительного ответа на RelayInfo
            ri.ack.received(RelayInfoReply(rid1, ri.ack, true))

            true
        }

        tr.addAdjacentRelayOverride(rid2, m)

        runBlocking { tr.ensureAdjacentRelaysHasLatestConfiguration().join() }

        // Повторная проверка должна быть проигнорирована, т.к. конфигурация не менялась
        runBlocking { tr.ensureAdjacentRelaysHasLatestConfiguration().join() }

        // Запрос должен быть только один
        verify(exactly = 1) { m.invoke(any()) }

        unmockkAll()
    }

    @Test
    fun replyToRelayInfo() {
        val rid1 = RelayID(1)
        val rid2 = RelayID(2)

        val tr = object : BaseRelay(rid1) {
            fun addAdjacentRelayOverride(id: RelayID, sink: RelayMessageToBoolean) = addAdjacentRelay(id, sink)
            fun receivedOverride(message: RelayMessage) = received(message)
        }

        val m = mockk<(RelayMessage) -> Boolean>()

        // Проверка отправки сообщения RelayInfoReply
        every { m.invoke(any()) } answers {
            val rm = it.invocation.args.first() as? RelayMessage
            val rir = rm?.payload as? RelayInfoReply

            if (rir != null) {
                assertEquals(rid1, rir.relay)
                true
            } else
                true
        }

        tr.addAdjacentRelayOverride(rid2, m)

        val ri = RelayInfo(rid2, arrayOf(), arrayOf(rid1), AckToken.empty, 0)
        tr.receivedOverride(RelayMessage(rid2, rid1, ri, 1U))

        verify { m.invoke(any()) }

        unmockkAll()
    }

    @Test
    @Timeout(2)
    fun testRelayInfoMessageTarget() {
        val rid1 = RelayID(1)
        val rid2 = RelayID(2)
        val rid3 = RelayID(3)

        val tr = object : BaseRelay(rid1) {
            fun addAdjacentRelayOverride(id: RelayID, sink: RelayMessageToBoolean) = super.addAdjacentRelay(id, sink)
            fun receivedOverride(message: RelayMessage) = received(message)
        }

        val m = mockk<(RelayMessage) -> Boolean>()

        every { m.invoke(any()) } answers {
            val rm = it.invocation.args.first() as RelayMessage
            val ri = rm.payload as RelayInfo
            assertEquals(rid1, ri.relay)
            assertEquals(rid3, rm.targetRelay)

            ri.ack.received(RelayInfoReply(rid3, ri.ack, true))

            true
        }

        tr.addAdjacentRelayOverride(rid2) { Message ->
            val mp = Message.payload

            if (mp is RelayInfo)
                mp.ack.received(RelayInfoReply(rid2, mp.ack, true))

            true
        }

        tr.addAdjacentRelayOverride(rid3, m)

        tr.receivedOverride(RelayMessage(rid2, rid1, RelayInfo(rid2, arrayOf(), arrayOf(), AckToken.empty, 0), 1U))

        runBlocking { tr.ensureAdjacentRelaysHasLatestConfiguration().join() }

        verify { m.invoke(any()) }

        unmockkAll()
    }

    @Test
    fun sendLocals() {
        // При отправке пакета присоединенным хостам ретранслятора (с множественными целями)
        // в цели получаемого получаемого пакета должен быть указан только сам получатель
        val rid1 = RelayID(1)

        val h1 = HostID(1)
        val h2 = HostID(2)
        val h3 = HostID(3)

        val heid1 = HostEndpointID(h1, 1U)
        val heid2 = HostEndpointID(h2, 2U)
        val heid3 = HostEndpointID(h3, 3U)

        val set = HostEndpointIDSet(arrayOf(heid1, heid2, heid3))

        val rtm = mockk<IRoutingTable>()

        every { rtm.updateRelayInfo(any(), any()) } returns true
        every { rtm.expand(any(), any()) } returns listOf(rid1 to set)

        val rcvr = mockk<(RelayEnvelope) -> Unit>()

        every { rcvr.invoke(any()) } returns Unit

        val tr = object : BaseRelay(rid1, rtm) {
        }

        tr.addAdjacentHost(h1, rcvr)
        tr.addAdjacentHost(h2, rcvr)
        tr.addAdjacentHost(h3, rcvr)

        tr.send(RelayEnvelope(HostEndpointID(h1, 0U), set, BytesPacket(byteArrayOf())))

        verify(exactly = 1) { rcvr.invoke(match { it.targets.size == 1 && it.targets.first() == heid1 }) }
        verify(exactly = 1) { rcvr.invoke(match { it.targets.size == 1 && it.targets.first() == heid2 }) }
        verify(exactly = 1) { rcvr.invoke(match { it.targets.size == 1 && it.targets.first() == heid3 }) }

        unmockkAll()
    }
}