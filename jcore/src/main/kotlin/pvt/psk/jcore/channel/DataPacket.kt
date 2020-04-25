package pvt.psk.jcore.channel

import pvt.psk.jcore.relay.*

/**
 * Сообщение с прикрепленными к нему метаданными
 */
abstract class DataPacket(metadata: Array<Any>? = null) : Message() {

    var tag : PacketTag = PacketTag.Empty

    val metadata: Array<Any>? =
            when {
                metadata != null && metadata.count() > 0 -> metadata
                else -> null
            }
}