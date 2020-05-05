package pvt.psk.jcore.utils

/**
 * Удаляет из коллекции первый элемент удовлетворяющий условию
 *
 * @param filter Проверяемое условие
 * @receiver Исходная коллекция
 * @return Кортеж содержащий удаленное значение (при наличии) и модифицированную коллекцию
 */
inline fun <T> Collection<T>.pickOutFirst(filter: (T) -> Boolean): Pair<T?, Collection<T>> {
    val v = firstOrNull(filter)

    return if (v == null)
        v to this
    else
        v to (this - v)
}