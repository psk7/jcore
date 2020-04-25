package pvt.psk.jcore.channel

/**
 * Сообщение, содержащее байтовый массив
 */
class BytesPacket(Data: ByteArray, Metadata: Array<Any>? = null) : DataPacket(Metadata) {

    val data: ByteArray = Data

    override fun toString(): String = "BytesPacket (l=${data.size})"
}