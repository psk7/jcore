package pvt.psk.jcore.relay

import pvt.psk.jcore.host.*
import java.util.concurrent.locks.*
import kotlin.concurrent.*

/**
 * Таблица маршрутизации
 */
class RoutingTable(private val source: RelayID) : IRoutingTable {

    data class Entry(var relay: RelayID, var revision: Int = 1,
                     var adjacentRelays: Array<RelayID>, var adjacentHosts: Array<HostID>)

    private val lock = ReentrantReadWriteLock()
    private val _table = HashMap<RelayID, Entry>()
    private var hostsMap: Map<HostID, RelayID> = mapOf()
    private val cache = HashMap<HostID, RelayID>()
    private val cache2 = HashMap<Pair<HostID, HostEndpointIDSet>, Collection<Pair<RelayID, HostEndpointIDSet>>>()

    override val table: Collection<Entry>
        get() = lock.read { _table.values }

    override val hosts: Collection<HostID>
        get() = lock.read { hostsMap.keys }

    init {
        if (source.isUnknown)
            throw Exception("source не может быть RelayID.Unknown")
    }

    private fun calculateHostsMap() {
        hostsMap = _table.flatMap { x -> x.value.adjacentHosts.map { y -> Pair(y, x.key) } }.associate { it }

        cache.clear()
        cache2.clear()
    }

    override fun updateRelayInfo(relayInfo: RelayInfo): Boolean {
        val r = relayInfo.relay

        if (r == source || r.isUnknown)
            return false

        lock.write {
            var f = false

            for (entry in relayInfo.list) {
                if (entry.relay == source)
                    continue

                val ee = _table[entry.relay]
                if (ee == null)
                    _table[entry.relay] = Entry(entry.relay, entry.revision, entry.adjacentRelays, entry.adjacentHosts)
                else {
                    if (entry.revision <= ee.revision)
                        continue

                    ee.relay = entry.relay
                    ee.revision = entry.revision
                    ee.adjacentHosts = entry.adjacentHosts
                    ee.adjacentRelays = entry.adjacentRelays
                }

                f = true
            }

            if (f)
                calculateHostsMap()

            return f
        }
    }

    override fun updateRelayInfo(hosts: Array<HostID>, relays: Array<RelayID>): Boolean = lock.write {
        val rl = relays.filter { x -> x != source }.sortedBy { it }.toTypedArray()
        val hl = hosts.sortedBy { it }.toTypedArray()

        val ee = _table[source]

        var f = false

        if (ee == null) {
            _table[source] = Entry(source, 2, rl, hl)
            f = true
        } else {
            if (!ee.adjacentHosts.contentEquals(hl)) {
                ee.adjacentHosts = hl
                f = true
            }

            if (!ee.adjacentRelays.contentEquals(rl)) {
                ee.adjacentRelays = rl
                f = true
            }

            if (f)
                ee.revision++
        }

        calculateHostsMap()

        return f
    }

    fun findNextHopForHost(target: HostID): RelayID = lock.write {
        val relayID = cache[target]
        if (relayID != null)
            return relayID

        val tr = hostsMap[target] ?: return RelayID.Unknown

        if (!_table.containsKey(tr))
            return RelayID.Unknown

        if (tr == source)
            return tr

        val field = _table
            .flatMap { x -> x.value.adjacentRelays.asIterable() }.distinct().associateWith { 0 }.toMutableMap()

        field[source] = 1  // Исходная точка маршрута
        field[tr] = 0      // Конечная точка маршрута

        var cnt = 1

        var touch: Boolean

        // Распространение волны
        do {
            touch = false

            for (kp in field.keys.toTypedArray()) {
                if (field[kp] != cnt)
                    continue

                val ar = _table[kp] ?: continue

                for (id in ar.adjacentRelays) {
                    if (field[id] != 0)
                        continue

                    field[id] = cnt + 1
                    touch = true
                }
            }

            cnt++
        } while (field[tr] == 0 && touch)

        if (!touch)
            return RelayID.Unknown // Цель недостижима

        // Построение обратного пути

        var rr = tr

        while (field[rr]!! > 2) {
            val i = field[rr]!!
            rr = _table[rr]!!.adjacentRelays.firstOrNull { x -> field[x] == i - 1 }!!
            if (rr.isUnknown)
                return RelayID.Unknown
        }

        cache[target] = rr

        return rr
    }

    override fun expand(from: HostID, targets: HostEndpointIDSet): Collection<Pair<RelayID, HostEndpointIDSet>> =
        lock.write {
            val key = from to targets

            var r = cache2[key]
            if (r != null)
                return r

            val hops = targets.expandBroadcasts { hostsMap.keys.filter { x -> x != from }.toList() }
                .map { hid -> hid to findNextHopForHost(hid.hostID) }
                .filter { x -> !x.second.isUnknown }

            r = hops.map { it.second }.distinct()
                .map { it to HostEndpointIDSet(hops.filter { i -> i.second == it }.map { i -> i.first }) }

            cache2[key] = r

            return r
        }
}