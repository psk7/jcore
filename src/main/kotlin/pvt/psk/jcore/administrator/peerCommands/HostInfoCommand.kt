package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.host.*

/**
 * Команда - информация о каналах хоста
 *
 * @param SequenceID - порядковый номер команды
 * @param FromHost - Идентификатор хоста, информация о котором содержится в команте
 * @param ToHost - Идетификатор хоста - получателя команды
 * @param endPoints - Список конечных точек каналов хоста отправителя команды
 * @param payload - Дополнительная информация о хосте
 */
open class HostInfoCommand(val SequenceID: Int, FromHost: HostID, val endPoints: Array<EndPointInfo>, ToHost: HostID,
                           vararg val payload: Any) :
        PeerCommand(CommandID.HostInfo, FromHost, ToHost) {

    private val _fins = mutableListOf<() -> Unit>()

    fun release() {
        _fins.forEach { it() }
        _fins.clear()
    }

    fun addFinalizer(f: () -> Unit) {
        synchronized(_fins) {
            _fins.add(f)
        }
    }

    override fun toString(): String = "HostInfo: From $fromHost, SeqID=$SequenceID"
}