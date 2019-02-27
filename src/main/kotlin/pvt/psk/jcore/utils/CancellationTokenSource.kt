package pvt.psk.jcore.utils

import kotlinx.coroutines.*
import java.time.*
import java.util.concurrent.*

class CancellationTokenSource {

    private var cancelreq = false
    private var rl = ConcurrentLinkedQueue<CancellationTokenRegistration>()

    constructor() {
    }

    constructor(delay: Duration) {
        cancelAfter(delay)
    }

    constructor(delay: Long) {
        cancelAfter(delay)
    }

    val token: CancellationToken
        get() = CancellationToken(this)

    fun cancelAfter(delay: Duration) = cancelAfter(delay.toMillis())

    fun cancelAfter(delay: Long) = GlobalScope.launch {
        if (cancelreq)
            return@launch

        delay(delay)

        this@CancellationTokenSource.cancel()
    }

    fun cancel() {
        if (cancelreq)
            return

        cancelreq = true

        rl.forEach { it.cancel() }
    }

    fun register(safeClose: () -> Unit): CancellationTokenRegistration {

        val r = CancellationTokenRegistration(this, safeClose)
        rl.add(r)

        if (cancelreq)
            safeClose()

        return r
    }

    val isCancellationRequested: Boolean
        get() = cancelreq
}