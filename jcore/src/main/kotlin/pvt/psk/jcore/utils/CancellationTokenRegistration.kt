package pvt.psk.jcore.utils

import kotlinx.coroutines.*

class CancellationTokenRegistration(@Suppress("unused") private val cancellationTokenSource: CancellationTokenSource, val safeClose: () -> Unit) {

    var closed = false

    fun cancel() {
        if (closed)
            return

        try {
            safeClose()
        } catch (e: Throwable) {

        }
    }

    fun close() {
        closed = true
    }
}