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
    } catch (e: TimeoutCancellationException) {
        false
    }
}

suspend fun <T> Deferred<T>.cancelSafeAwait(): T? =
        try {
            await()
        } catch (e: CancellationException) {
            null
        }
