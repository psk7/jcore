package pvt.psk.jcore.channel

interface ISender {
    fun send(Packet: DataPacket, Target: EndPoint)
}