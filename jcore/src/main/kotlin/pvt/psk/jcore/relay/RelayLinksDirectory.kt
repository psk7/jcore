package pvt.psk.jcore.relay

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*
import java.util.concurrent.*
import java.util.concurrent.locks.*
import kotlin.concurrent.*

private typealias cacheKey = Pair<HostID, HostEndpointIDSet>

private typealias cacheLcl = Pair<(RelayEnvelope) -> Unit, HostEndpointIDSet>
private typealias cacheRem = Triple<RelayID, (RelayMessage) -> Unit, HostEndpointIDSet>
private typealias cacheItem = Pair<Iterable<cacheLcl>, Iterable<cacheRem>>

class RelayLinksDirectory {

    private val _adjacentHosts = ConcurrentHashMap<HostID, RelayEnvelopeToUnit>()
    private val _adjacentRelays = ConcurrentHashMap<RelayID, RelayMessageToUnit>()
    private val _assocMap = ConcurrentPriorityHashMap<HostID, RelayID>()

    private val cache = ConcurrentHashMap<cacheKey, cacheItem>()

    private val lock = ReentrantReadWriteLock()

    /**
     * Список граничащих ретрансляторов
     */
    val adjacentRelays
        get() = _adjacentRelays

    val remoteHosts
        get() = lock.read { _assocMap.keys.toTypedArray() }

    /**
     * Список присоединенных хостов
     */
    val adjacentHosts
        get() = _adjacentHosts

    fun addAdjacentHost(id: HostID, sink: (RelayEnvelope) -> Unit): Boolean = lock.write {
        cache.clear()

        return _adjacentHosts.put(id, sink) == null
    }

    fun addAdjacentRelay(id: RelayID, sink: (RelayMessage) -> Unit): Boolean = lock.write {
        cache.clear()

        return _adjacentRelays.put(id, sink) == null
    }

    /**
     * Удаляет ретранслятор из списка
     *
     * @param id Идентификатор удаляемого ретранслятора
     * @return [true] - если ретранслятор был удален, [false] - если такого ретранслятора в списке не было
     */
    fun removeAdjacentRelay(id: RelayID):Boolean = lock.write {
        cache.clear()
        return _adjacentRelays.remove(id) != null
    }

    fun expandTarget(from: HostID, targets: HostEndpointIDSet): cacheItem = lock.read {

        val cv = cache[from to targets]

        if (cv != null)
            return cv

        val t = targets.expandBroadcasts { _adjacentHosts.keys.union(_assocMap.keys).distinct().filter { it != from } }

        val l = _adjacentHosts.filter { t.containsHost(it.key) }.map { it.value to t.filter(it.key) }

        val r = mutableListOf<cacheRem>()

        for (gv in _assocMap.groupByValues(t::containsHost))
            r.add(Triple(gv.key, _adjacentRelays[gv.key] ?: continue, t.filterHosts(gv.value)))

        val v = l to r

        cache[from to targets] = v

        return v
    }

    fun addOrUpdate(hostID: HostID, distance: Int, relayID: RelayID): Boolean = lock.write {
        cache.clear()

        return _assocMap.addOrUpdate(hostID, distance, relayID)
    }

    fun groupByValues(): Map<RelayID, List<HostID>> = _assocMap.groupByValues()

    fun groupByValues(filter: (HostID) -> Boolean): Map<RelayID, List<HostID>> = _assocMap.groupByValues(filter)
}