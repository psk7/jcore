package pvt.psk.jcore.utils

import kotlinx.coroutines.*

fun <T> Deferred<T>.wait() = runBlocking { await() }

fun <T> Job.continueWith(block: () -> T): Deferred<T> {
    val r = CompletableDeferred<T>()

    invokeOnCompletion { r.complete(block()) }

    return r
}