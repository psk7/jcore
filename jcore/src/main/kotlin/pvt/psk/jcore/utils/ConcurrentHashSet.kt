package pvt.psk.jcore.utils

import java.util.concurrent.*

class ConcurrentHashSet<T> : Set<T> {

    private val map = ConcurrentHashMap<T, Unit>()

    override fun contains(element: T): Boolean {
        return map.containsKey(element)
    }

    override val size: Int
        get() = map.size

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { map.contains(it) }

    override fun isEmpty(): Boolean = map.isEmpty()

    override fun iterator(): Iterator<T> = map.keys().iterator()

    /**
     * Добавляет новое значение
     *
     * @return [true] если значение было добавлено, [false] если значение уже существовало в наборе
     */
    fun add(value: T) = map.put(value, Unit) == null

    /**
     * Удаляет значение из набора
     *
     * @return [true] если значение было удалено, [false] если значения не существовало в наборе
     */
    fun remove(value: T) = map.remove(value) != null
}