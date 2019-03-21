package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*

/**
 * Информация о конечной точке канала на удаленном хосте
 * @param channelName Имя канала
 * @param target Идентификатор удаленного хоста
 * @param payload Дополнительная информация о конечной точке
 */
class EndPointInfo(val target: HostID, val channelName: String, val dontSendTo: Boolean, private vararg val payload: Any) {
    fun get(Pos: Int): Any = payload[Pos]

}