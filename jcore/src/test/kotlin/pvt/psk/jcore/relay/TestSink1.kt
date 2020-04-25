package pvt.psk.jcore.relay

class TestSink1 : IHostEndpointSink {

    private val _send: ((RelayMessage) -> Unit)?

    constructor(Send: ((RelayMessage) -> Unit)? = null) {
        _send = Send
    }

    override fun send(message: RelayMessage) {
        _send?.invoke(message)
    }
}