package pvt.psk.jcore.relay

import kotlinx.coroutines.*
import org.koin.core.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.util.concurrent.*
import kotlin.coroutines.*

typealias RelayEnvelopeToUnit = (RelayEnvelope) -> Unit
typealias RelayMessageToUnit = (RelayMessage) -> Unit

abstract class BaseRelay(val relayID: RelayID) : IRelay, CoroutineScope, KoinComponent {

    protected val job: Job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    val logger by inject<Logger>()
    private val debugHolder: DebugInfoHolder by inject()

    val maxTTL = 16u

    protected val logCat: String = "Relay"

    private val links = RelayLinksDirectory()
    private val onRouteChangedNotifiers = ConcurrentHashMap<HostID, () -> Unit>()

    init {
        logger.writeLog(LogImportance.Trace, logCat, "Создан ретранслятор $relayID")
    }

    val adjacentRelays
        get() = links.adjacentRelays.keys.toTypedArray()

    val remoteHosts
        get() = links.remoteHosts

    override fun addAdjacentHost(id: HostID, sink: (RelayEnvelope) -> Unit, onRouteChanged: (() -> Unit)?) {

        if (onRouteChanged != null)
            onRouteChangedNotifiers[id] = onRouteChanged

        if (links.addAdjacentHost(id, sink))
            sendRelayInfo()
    }

    protected fun addAdjacentRelay(id: RelayID, sink: RelayMessageToUnit) {
        links.addAdjacentRelay(id, sink)

        onRouteChangedNotifiers.forEach { it.value() }
    }

    /**
     * Удаление регистрации граничащего ретранслятора
     *
     * @param id Идентификатор удаляемого граничащего ретранслятора
     */
    protected fun removeAdjacentRelay(id: RelayID) {
        links.removeAdjacentRelay(id)

        onRouteChangedNotifiers.forEach { it.value() }
    }

    /**
     * Маршрутизация пакета от присоединенного хоста соседям/наружу.
     *
     * В списке целей RelayEnvelope не должно быть HostID.All.
     */
    override fun send(data: RelayEnvelope) {

        val (l, r) = links.expandTarget(data.from.hostID, data.targets)

        for ((sink, tgts) in l)
            sink(RelayEnvelope(data.from, tgts, data.payload))

        for ((from, sink, tgts) in r)
            sink(RelayMessage(relayID, from, RelayEnvelope(data.from, tgts, data.payload), 1u, null))
    }

    override fun addUplinkRelay(relay: BaseRelay) {
        addAdjacentRelay(relay.relayID) {
            if (it.ttl <= maxTTL && it.targetRelay != relayID)
                relay.received(RelayMessage(relayID, relay.relayID, it.payload, it.ttl, it.relaysTokens))
        }
    }

    /**
     * Маршрутизация пакета, пришедшего извне, присоединенным хостам и ретрансляция далее при необходимости
     */
    protected fun received(message: RelayMessage) {
        // message.targetRelay всегда должен указывать на следующего получателя в цепочке пересылки
        // иначе это какой-то неправильный пакет
        if (message.targetRelay != relayID || message.isRelayedThrough(relayID))
            return

        val mp = message.payload

        val l = {
            if (mp is RelayEnvelope) {
                for (tid in mp.targets)
                    links.adjacentHosts[tid.hostID]?.invoke(RelayEnvelope(mp.from, mp.targets.filter(tid.hostID), mp.payload))
            }
        }

        if (mp is RelayInfo)
            onRelayInfo(mp, message.source, message.ttl)

        if (message.ttl.toInt() > maxTTL.toInt()) {
            executeReceivers(l, null)
            return
        }

        if (mp is RelayInfo)       // Ретрансляция информации о ретрансляторе (всем окружающим за исключением отправителя)
            for (kp in links.adjacentRelays.filter { it.key != message.source })
                kp.value(RelayMessage(relayID, kp.key, mp, message.ttl + 1u, message.relaysTokens))

        if (mp !is RelayEnvelope) {
            executeReceivers(l, null)
            return
        }

        val r = {
            for (gv in links.groupByValues(mp.targets::containsHost)) {
                logger.writeLog(LogImportance.Trace, logCat,
                                "Ретрансляция $relayID->${gv.key} (From:${mp.from}, Payload:$mp)")

                links.adjacentRelays[gv.key]?.invoke(RelayMessage(relayID, gv.key,
                                                                  RelayEnvelope(mp.from, mp.targets.filterHosts(gv.value), mp.payload),
                                                                  message.ttl + 1u, message.relaysTokens))
            }
        }

        executeReceivers(l, r)
    }

    protected fun executeReceivers(locals: (() -> Unit)?, remotes: (() -> Unit)?) {
        locals?.invoke()
        remotes?.invoke()
    }

    /**
     * Обработка информации о ретрансляторах
     */
    private fun onRelayInfo(relayInfo: RelayInfo, id: RelayID, distance: UInt) {

        if (id == relayID || relayInfo.relayID == relayID)   // Пакеты от себя или о себе отбрасываются
            return

        var needSend = false

        relayInfo.adjacentHosts.forEach {
            needSend = needSend or links.addOrUpdate(it, distance.toInt(), id)
        }

        if (needSend)
            sendRelayInfo()
    }

    /**
     * Отправка информации о своих присоединенных хостах всем граничащим ретрансляторам
     */
    fun sendRelayInfo() {
        val ri = RelayInfo(relayID, links.adjacentHosts.keys().toList().toTypedArray(), false)

        logger.writeLog(LogImportance.Trace, logCat, "Отправка информации о ретрансляторе (${links.adjacentHosts.size})")

        if (links.adjacentRelays.size == 0)
            logger.writeLog(LogImportance.Trace, logCat, "Список целей пуст в sendRelayInfo")

        links.adjacentRelays.forEach {
            it.value(RelayMessage(relayID, it.key, ri, 1U, null))
        }
    }

    open fun close() {
        runBlocking { job.cancelAndJoin() }
    }
}