package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

/**
 * Команда проверки доступности хоста
 * @param fromHost Идентификатор хоста-отправителя команды
 * @param toHost Идентификатор хоста-получателя команды
 * @param token Метка команды
 */
open class PingCommand(fromHost: HostID, toHost: HostID, val token: AckToken) : PeerCommand(CommandID.Ping, fromHost, toHost)
