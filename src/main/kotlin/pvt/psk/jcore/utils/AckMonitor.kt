package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import java.util.concurrent.*

val lst = ConcurrentHashMap<AckToken, CompletableDeferred<Any>>()

fun <T> register(timeOut: Int): Pair<AckToken, Deferred<T>> =
        register(CancellationTokenSource(timeOut.toLong()).token)

fun <T> register(cancellationToken: CancellationToken): Pair<AckToken, Deferred<T>> =
        register<T>().apply {
            cancellationToken.register {
                second.cancel()
            }
        }

@Suppress("UNCHECKED_CAST")
fun <T> register(): Pair<AckToken, Deferred<T>> {
    val tk = AckToken()
    val cd = CompletableDeferred<Any>()

    lst[tk] = cd

    return Pair(tk, GlobalScope.async(Dispatchers.Unconfined) { cd.await() as T })
}

fun <T> AckToken.received(Data: T) {
    val cd = lst[this] ?: return

    cd.complete(Data as Any)
    lst.remove(this)
}
