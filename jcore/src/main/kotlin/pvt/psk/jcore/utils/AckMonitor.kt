package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import org.joda.time.*
import java.util.concurrent.*

private class Bag(val result: CompletableDeferred<Any>, val start: DateTime, val measure: CompletableDeferred<Duration>)

private val lst = ConcurrentHashMap<AckToken, Bag>()

fun <T> register(timeOut: Int): Pair<AckToken, Deferred<T>> =
        register(CancellationTokenSource(timeOut.toLong()).token)

fun <T> register(cancellationToken: CancellationToken): Pair<AckToken, Deferred<T>> =
        register<T>().apply {
            cancellationToken.register {
                val bag = lst[first]!!

                bag.measure.complete(Duration(DateTime.now().millis - bag.start.millis))
                bag.result.cancel()
                lst.remove(first)
            }
        }

@Suppress("UNCHECKED_CAST")
fun <T> register(): Pair<AckToken, Deferred<T>> {
    val tk = AckToken()
    val cd = CompletableDeferred<Any>()
    val tc = CompletableDeferred<Duration>()

    lst[tk] = Bag(cd, DateTime.now(), tc)

    return tk to GlobalScope.async(Dispatchers.IO) { cd.await() as T }
}

fun <T> AckToken.received(Data: T) {
    val bag = lst[this] ?: return

    bag.measure.complete(Duration(DateTime.now().millis - bag.start.millis))
    bag.result.complete(Data as Any)
    lst.remove(this)
}

fun AckToken.getMeasure() = lst[this]?.measure

