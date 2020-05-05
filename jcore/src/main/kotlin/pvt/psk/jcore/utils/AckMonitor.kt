@file:Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")

package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.CancellationException
import org.joda.time.*
import java.util.concurrent.*

private class Bag(val result: CompletableDeferred<Any>, val timeOut: Int,
                  val start: DateTime, val measure: CompletableDeferred<Duration>)

private val lst = ConcurrentHashMap<AckToken, Bag>()

inline fun registerAckToken(timeOut: TimeSpan): AckToken = registerAckToken(timeOut.milliseconds.toInt())

fun registerAckToken(timeOut: Int = -1) = AckToken().also {
    lst[it] = Bag(CompletableDeferred(), timeOut, DateTime.now(), CompletableDeferred())
}

suspend fun <T> AckToken.await(): T? {
    val bag = lst[this] ?: return null
    val r = bag.result

    return try {
        if (bag.timeOut == -1)
            r.await()
        else
            withTimeout(bag.timeOut.toLong()) { r.await() }
    }
    catch (e: CancellationException) {
        bag.measure.complete(Duration(DateTime.now().millis - bag.start.millis))
        bag.result.cancel()
        null
    }.also { lst.remove(this) } as T?
}

fun <T> AckToken.received(Data: T) {
    val bag = lst[this] ?: return

    bag.measure.complete(Duration(DateTime.now().millis - bag.start.millis))
    bag.result.complete(Data as Any)
}

fun AckToken.unregister() {
    lst.remove(this)
}

val AckToken.measure
    get() = lst[this]?.measure

