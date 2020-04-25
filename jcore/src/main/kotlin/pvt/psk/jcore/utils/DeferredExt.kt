package pvt.psk.jcore.utils

import kotlinx.coroutines.*

val <T> Deferred<T>.result: T
    get() = runBlocking { await() }

fun <T> Deferred<T>.wait() = runBlocking { await() }

fun <T> Deferred<T>.wait(millis: Int) = runBlocking {

    try {
        withTimeout(millis.toLong()) {
            await()
            true
        }
    }
    catch (e: TimeoutCancellationException) {
        false
    }
}

suspend fun <T> Deferred<T>.cancelSafeAwait(): T? =
    try {
        await()
    }
    catch (e: CancellationException) {
        null
    }

suspend fun <O, T> O.cancelSafe(block: suspend O.() -> T): T? =
    try {
        block()
    }
    catch (ex: CancellationException) {
        null
    }

fun <T> Job.continueWith(block: () -> T): Deferred<T> {
    val r = CompletableDeferred<T>()

    invokeOnCompletion { r.complete(block()) }

    return r
}

fun <E, T> Deferred<E>.continueWith(block: (p: Deferred<E>) -> T): Deferred<T> {
    val r = CompletableDeferred<T>()

    invokeOnCompletion { r.complete(block(this)) }

    return r
}