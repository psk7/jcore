package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*

/**
 * Базовый класс сообщений обмена
 * @param fromHost Идентификатор хоста отправителя сообщения
 * @param toHost Идентификатор хоста получателя сообщения
 */
abstract class DirectedMessage(val fromHost: HostID, val toHost: HostID) : Message() {
}