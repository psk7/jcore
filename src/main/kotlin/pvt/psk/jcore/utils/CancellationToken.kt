package pvt.psk.jcore.utils

class CancellationToken {

    companion object {
        private val none = CancellationToken()

        val None: CancellationToken
            get() = none
    }

    fun register(safeClose: () -> Unit): CancellationTokenRegistration = source?.register(safeClose)
            ?: CancellationTokenRegistration(CancellationTokenSource()) {}.also {
                it.close()
            }

    internal constructor(source: CancellationTokenSource?) {
        this.source = source
    }

    constructor() {
        this.source = null
    }

    private val source: CancellationTokenSource?

    val isCancellationRequested: Boolean
        get() = source?.isCancellationRequested ?: false

}

fun CancellationToken.getSafeToken(): CancellationTokenSource {
    val cts = CancellationTokenSource()

    register {
        try {
            cts.cancel()
        } catch (e: Exception) {
        }
    }

    return cts
}