package pvt.psk.jcore.administrator.peerCommands

import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*

/**
 * Базовая команда обмена информацией о соседних хостах
 * @param CommandID Идентификатор команды
 * @param fromHost Идентификатор хоста-отправителя команды
 * @param toHost Идентификатор хоста-получателя команды
 */
abstract class PeerCommand(val CommandID: CommandID, fromHost: HostID, toHost: HostID) : DirectedMessage(fromHost, toHost) {

    private val _ct = CompletableDeferred<Unit>()

    val completion: Deferred<Unit>
        get() = _ct

    fun complete() {
        _ct.complete(Unit)
    }
}
