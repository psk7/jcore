package pvt.psk.jcore.channel

import kotlinx.atomicfu.*
import java.util.concurrent.*

private val ids = atomic(1)

/**
 * Маршрутизатор сообщений
 */
class Router : IChannel {

    private val lst = ConcurrentHashMap<Int, LocalChannel>()

    class MessageBag(val instanceID: Int, val Message: Message)

    //region LocalChannel
    class LocalChannel(val Router: Router,
                       val ID: Int,
                       val received: DataReceived?,
                       val description: String?) : IChannelEndPoint {

        private var _isClosed: Boolean = false

        override fun sendMessage(message: Message) {
            if (_isClosed)
                return

            Router.onReceive(MessageBag(ID, message))
        }

        fun invoke(Packet: Message) {
            if (_isClosed)
                Router.removeChannel(ID)
            else
                received?.invoke(this, Packet)
        }

        override fun close() {
            _isClosed = true
        }
    }
    //endregion

    private fun onReceive(Packet: MessageBag) {

        val pid = Packet.instanceID
        val pm = Packet.Message

        lst.filter { it.key != pid }.values.forEach { it.invoke(pm) }
    }

    override fun sendMessage(message: Message) = lst.values.forEach { it.invoke(message) }

    private fun removeChannel(ID: Int) {
        lst.remove(ID)
    }

    override fun getChannel(received: DataReceived?, description: String?): IChannelEndPoint {

        val id = ids.getAndIncrement()

        val lc = LocalChannel(this, id, received, description)

        lst[id] = lc

        return lc
    }

    override fun close() {}
}