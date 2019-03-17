package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*

/**
 * Команда ответа на запрос проверки доступности хоста
 * @param fromHost Идентификатор хоста-отправителя команды
 * @param toHost Идентификатор хоста-получателя команды
 * @param token Метка команды
 */
open class PingReplyCommand(fromHost: HostID, toHost: HostID, val token: AckToken) : PeerCommand(CommandID.PingReply, fromHost, toHost)
