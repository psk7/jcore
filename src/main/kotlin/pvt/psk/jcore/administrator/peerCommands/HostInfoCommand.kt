package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*

/**
 * Команда - информация о каналах хоста
 *
 * @param FromHost - Идентификатор хоста, информация о котором содержится в команте
 * @param ToHost - Идетификатор хоста - получателя команды
 * @param endPoints - Список конечных точек каналов хоста отправителя команды
 * @param payload - Дополнительная информация о хосте
 */
open class HostInfoCommand(FromHost: HostID, val endPoints: Array<EndPointInfo>, ToHost: HostID, vararg val payload: Any) :
        PeerCommand(CommandID.HostInfo, FromHost, ToHost) {

    override fun toString(): String = "HostInfo: From $fromHost, SeqID=$sequence"
}