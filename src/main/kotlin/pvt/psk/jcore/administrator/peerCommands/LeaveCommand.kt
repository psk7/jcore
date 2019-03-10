package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*

/**
 * Команда - уведомление о завершении работы удаленного хоста
 * @param fromHost Идентификатор хоста-отправителя команды
 * @param toHost Идентификатор хоста-получателя команды
 */
class LeaveCommand(fromHost: HostID, toHost: HostID) : PeerCommand(CommandID.Leave, fromHost, toHost) {
    override fun toString(): String = "Leave Host=$fromHost"
}
