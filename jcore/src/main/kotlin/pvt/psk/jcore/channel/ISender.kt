package pvt.psk.jcore.channel

interface ISender {
    fun send(packet: DataPacket, target: EndPoint)
}