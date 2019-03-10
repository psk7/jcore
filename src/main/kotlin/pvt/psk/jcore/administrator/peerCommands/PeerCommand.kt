package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*

/**
 * Базовая команда обмена информацией о соседних хостах
 * @param CommandID Идентификатор команды
 * @param fromHost Идентификатор хоста-отправителя команды
 * @param toHost Идентификатор хоста-получателя команды
 */
abstract class PeerCommand(val CommandID: CommandID, fromHost: HostID, toHost: HostID) : Message(fromHost, toHost)
