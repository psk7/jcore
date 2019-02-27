package pvt.psk.jcore.utils

class CancellationToken {

    companion object {
        private val none = CancellationToken()

        val None: CancellationToken
            get() = none
    }

    fun register(safeClose: () -> Unit) = source!!.register(safeClose)

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