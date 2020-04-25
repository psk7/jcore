package pvt.psk.jcore.utils

import java.util.concurrent.*

class ConcurrentPriorityHashMap<TK, TV> {

    private val dictionary = ConcurrentHashMap<TK, ConcurrentHashMap<Int, TV>>()

    val keys
        get() = dictionary.keys().toList()

    val values
        get() = dictionary.values.flatMap { it.values }.distinct().toList()

    fun tryGetValue(key: TK): TV? {
        val cc = dictionary[key] ?: return null

        return cc[cc.keys().asSequence().min()] ?: return null
    }

    fun addOrUpdate(key: TK, priority: Int, value: TV): Boolean {
        val d = dictionary.getOrPut(key) { ConcurrentHashMap<Int, TV>() }

        if (d.putIfAbsent(priority, value) == null)
            return true

        d[priority] = value

        return false
    }

    operator fun get(key: TK): TV? {
        val d = dictionary.getOrPut(key) { ConcurrentHashMap<Int, TV>() }

        var m: Int? = null

        for (v in d.keys())
            if (m == null || v < m)
                m = v

        if (m == null)
            return null

        return d[m]
    }

    fun set(key: TK, priority: Int, value: TV) {
        val d = dictionary.getOrPut(key) { ConcurrentHashMap<Int, TV>() }
        d[priority] = value
    }

    fun remove(key: TK) {
        dictionary.remove(key)
    }

    fun remove(key: TK, priority: Int): Pair<Boolean, TV?> {
        val d = dictionary.getOrPut(key) { ConcurrentHashMap<Int, TV>() }

        val rv = d.remove(priority)

        return Pair(rv == null, rv)
    }

    fun removeValue(value: TV) {
        val ds = dictionary.values.toTypedArray()

        for (it in ds)
            for (ie in it)
                if (ie.value == value)
                    it.remove(ie.key)
    }

    fun groupByValues(): Map<TV, List<TK>> =
        dictionary.asSequence().map { it.key to get(it.key) }.filter { it.second != null }.groupBy({ it.second!! }, { it.first })

    fun groupByValues(filter: (TK) -> Boolean): Map<TV, List<TK>> =
        dictionary.asSequence().filter { filter(it.key) }.map { it.key to get(it.key) }.filter { it.second != null }
            .groupBy({ it.second!! }, { it.first })
}