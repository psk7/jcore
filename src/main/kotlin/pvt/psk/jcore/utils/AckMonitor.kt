package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import pvt.psk.jcore.utils.AckToken.Companion.New
import java.util.concurrent.*

val _lst = ConcurrentHashMap<AckToken, CompletableDeferred<Any>>()

fun <T> register(): Pair<AckToken, Deferred<T>> {
    val tk = New()
    val cd = CompletableDeferred<Any>()

    _lst[tk] = cd

    return Pair(tk, GlobalScope.async { cd.await() as T })
}

fun <T> received(Token: AckToken, Data: T) {
    val cd = _lst[Token] ?: return

    cd.complete(Data as Any)
    _lst.remove(Token)
}
