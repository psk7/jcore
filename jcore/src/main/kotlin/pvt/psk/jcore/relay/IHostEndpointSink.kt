package pvt.psk.jcore.relay

interface IHostEndpointSink {

    fun send(message: RelayMessage)
}