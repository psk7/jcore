package pvt.psk.jcore.utils

class CancellationToken {

    companion object {
        private val _none = CancellationToken()

        val None: CancellationToken
            get() = _none
    }

    fun register(safeClose: () -> Unit) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val isCancellationRequested: Boolean
        get() = false

}