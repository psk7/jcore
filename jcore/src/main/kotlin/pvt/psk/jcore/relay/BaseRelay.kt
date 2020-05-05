package pvt.psk.jcore.relay

import kotlinx.coroutines.*
import org.koin.core.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.concurrent.*
import kotlin.coroutines.*

typealias RelayEnvelopeToUnit = (RelayEnvelope) -> Unit
typealias RelayMessageToBoolean = (RelayMessage) -> Boolean

abstract class BaseRelay(val relayID: RelayID, routingTable: IRoutingTable? = null)
    : IRelay, CoroutineScope, KoinComponent {

    protected val job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    val logger by inject<Logger>()
    private val debugHolder: DebugInfoHolder by inject()

    val maxTTL = 16u

    protected val logCat: String = "Relay"

    private val onRouteChangedNotifiers = ConcurrentHashMap<HostID, () -> Unit>()

    private val adjHosts = ConcurrentHashMap<HostID, RelayEnvelopeToUnit>()
    private val adjRelays = ConcurrentHashMap<RelayID, RelayMessageToBoolean>()

    @Suppress("NOTHING_TO_INLINE")
    private inline fun RelayID?.sink(message: RelayMessage): Boolean {
        return adjRelays[this ?: return false]?.invoke(message) ?: return false
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun HostID?.sink(message: RelayEnvelope) {
        adjHosts[this ?: return]?.invoke(message)
    }

    init {
        logger.writeLog(LogImportance.Trace, logCat, "Создан ретранслятор $relayID")
    }

    /**
     * Список граничащих ретрансляторов
     */
    val adjacentRelays
        get() = adjRelays.keys.toTypedArray()

    /**
     * Список присоединенных хостов
     */
    val adjacentHosts
        get() = adjHosts.keys.toTypedArray()

    /**
     * Список хостов за исключением локальных
     */
    val remoteHosts
        get() = (routingTable.hosts - adjHosts.keys).toTypedArray()

    protected val adjacentRelayReceiveRelayInfoConfirm = RevisionEnsurator(::sendRelayInfo, 60.seconds)

    protected val routingTable: IRoutingTable = routingTable ?: RoutingTable(relayID)

    override fun addAdjacentHost(id: HostID, sink: (RelayEnvelope) -> Unit, onRouteChanged: (() -> Unit)?) {
        if (adjHosts.putIfAbsent(id, sink) == null) {
            if (onRouteChanged != null)
                onRouteChangedNotifiers[id] = onRouteChanged

            routingTable.updateRelayInfo(adjacentHosts, adjacentRelays)

            adjacentRelayReceiveRelayInfoConfirm.bumpRevision()
        }

        ensureAdjacentRelaysHasLatestConfiguration()
    }

    fun removeAdjacentHost(id: HostID) {
        if (adjHosts.remove(id) != null) {
            routingTable.updateRelayInfo(adjacentHosts, adjacentRelays)

            adjacentRelayReceiveRelayInfoConfirm.bumpRevision()
        }

        ensureAdjacentRelaysHasLatestConfiguration()
    }

    protected fun addAdjacentRelay(id: RelayID, sink: RelayMessageToBoolean) {
        if (adjRelays.putIfAbsent(id, sink) == null) {
            routingTable.updateRelayInfo(adjacentHosts, adjacentRelays)

            adjacentRelayReceiveRelayInfoConfirm.bumpRevision()

            onRouteChangedNotifiers.forEach { it.value() }
        }

        ensureAdjacentRelaysHasLatestConfiguration()
    }

    /**
     * Удаление регистрации граничащего ретранслятора
     *
     * @param id Идентификатор удаляемого граничащего ретранслятора
     */
    protected fun removeAdjacentRelay(id: RelayID) {
        if (adjRelays.remove(id) != null) {
            routingTable.updateRelayInfo(adjacentHosts, adjacentRelays)
            logger.writeLog(LogImportance.Info, logCat, "Ретранслятор отключился $id")

            adjacentRelayReceiveRelayInfoConfirm.bumpRevision()

            onRouteChangedNotifiers.forEach { it.value() }
        }

        ensureAdjacentRelaysHasLatestConfiguration()
    }

    /**
     * Маршрутизация пакета от присоединенного хоста соседям/наружу.
     *
     * В списке целей RelayEnvelope не должно быть HostID.All.
     */
    override fun send(data: RelayEnvelope) {
        val (local, remotes) = routingTable.expand(data.from.hostID, data.targets).pickOutFirst { it.first == relayID }

        remotes.forEach { (relay, targets) ->
            relay.sink(RelayMessage(relayID, relay, RelayEnvelope(data.from, targets, data.payload), 1u))
        }

        local?.second?.forEach {
            it.hostID.sink(RelayEnvelope(data.from, local.second.filter(it.hostID), data.payload))
        }
    }

    override fun addUplinkRelay(relay: BaseRelay) =
        addAdjacentRelay(relay.relayID) r@{
            if (it.ttl > maxTTL || it.targetRelay == relayID)
                return@r false

            relay.received(RelayMessage(relayID, relay.relayID, it.payload, it.ttl))
            return@r true
        }

    /**
     * Маршрутизация пакета, пришедшего извне, присоединенным хостам и ретрансляция далее при необходимости
     */
    protected fun received(message: RelayMessage) {
        // message.targetRelay всегда должен указывать на следующего получателя в цепочке пересылки
        // иначе это какой-то неправильный пакет
        if (message.targetRelay != relayID)
            return

        val mp = message.payload

        if (mp is RelayInfo) {
            onRelayInfo(mp)
            return
        } else if (mp is RelayInfoReply) {
            adjacentRelayReceiveRelayInfoConfirm.replyReceived(mp)
            return
        }

        if (mp !is RelayEnvelope)
            return

        val (local, remotes) = routingTable.expand(mp.from.hostID, mp.targets).pickOutFirst { it.first == relayID }

        // Ретрансляция
        if (message.ttl.toInt() <= maxTTL.toInt()) {
            for ((relay, targets) in remotes) {
                if (relay == message.source) // Обратно сообщения не отправляются
                    continue

                logger.writeLog(LogImportance.Trace, logCat,
                                "Ретрансляция $relayID->${relay} (From:${mp.from}, Payload:$mp)")

                relay.sink(RelayMessage(relayID, relay, RelayEnvelope(mp.from, targets, mp.payload), message.ttl + 1u))
            }
        }

        // Отправка хостам
        local?.second?.forEach {
            it.hostID.sink(RelayEnvelope(mp.from, local.second.filter(it.hostID), mp.payload))
        }
    }

    /**
     * Обработка информации о ретрансляторах
     */
    private fun onRelayInfo(relayInfo: RelayInfo) {
        if (relayInfo.relay == relayID)   // Пакеты от себя или о себе отбрасываются
            return

        val ar = routingTable.updateRelayInfo(relayInfo)

        val reply = RelayInfoReply(relayID, relayInfo.ack, ar)

        relayInfo.relay.sink(RelayMessage(relayID, relayInfo.relay, reply, 1U))

        if (!ar)
            return

        adjacentRelayReceiveRelayInfoConfirm.bumpRevision()

        ensureAdjacentRelaysHasLatestConfiguration()
    }

    private fun sendRelayInfo(relay: RelayID, token: AckToken): Boolean {
        val sink = adjRelays[relay] ?: return false

        sink(RelayMessage(relayID, relay, RelayInfo(relayID, routingTable.table, token), 1U))

        return true
    }

    /**
     * Ожидание передачи всем ретрансляторам актуальной конфигурации
     *
     * @return Список ретрансляторов не подтвердивших получение конфигурации за отведенное время
     */
    fun ensureAdjacentRelaysHasLatestConfiguration() = launch {
        ensureAdjacentRelaysHasLatestConfiguration(adjacentRelays)
    }

    /**
     * Ожидание передачи всем ретрансляторам актуальной конфигурации
     *
     * @param relays Список ретрансляторов, получающих данные об актуальной конфигурации
     *
     * @return Список ретрансляторов не подтвердивших получение конфигурации за отведенное время
     */
    suspend fun ensureAdjacentRelaysHasLatestConfiguration(relays: Array<RelayID>): Array<RelayID> =
        adjacentRelayReceiveRelayInfoConfirm.ensure(relays).toTypedArray()

    open fun close() {
        runBlocking { job.cancelAndJoin() }
    }
}