package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

/**
 * Сообщение с прикрепленными к нему метаданными
 */
abstract class DataPacket(fromHost: HostID, toHost: HostID, metadata: Array<Any>? = null) : DirectedMessage(fromHost, toHost) {

    /**
     * Метки пакета данных
     */
    var tags: Array<String>? = null

    val metadata: Array<Any>? =
            when {
                metadata != null && metadata.count() > 0 -> metadata
                else -> null
            }
}