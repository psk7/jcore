package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*
import java.util.concurrent.*

class Router
{
    companion object
    {
        private var _ids: Int = 1
    }

    val _lst: ConcurrentMap<Int, LocalChannel> = ConcurrentHashMap()

    class MessageBag(val instanceID: Int, val Message: Message)

    //region LocalChannel
    class LocalChannel(val Router: Router, val ID: Int, val enableFeedback: Boolean, val acceptHost: HostID?, description: String?) : IChannelControl
    {
        val r = Event<DataReceived>()

        override val received: Event<DataReceived>
            get() = r

        private var _isClosed: Boolean = false

        override fun sendMessage(Data: Message)
        {
            if (_isClosed)
                return

            val b = MessageBag(ID, Data);

            Router.onReceive(b, enableFeedback)
        }

        fun invoke(Packet: Message)
        {
            received(DataReceived(this, Packet))
        }

        override fun close()
        {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
    //endregion

    private fun onReceive(Packet: MessageBag, enableFeedback: Boolean)
    {
        val pid = Packet.instanceID

        val l = _lst.filter { it.key != pid || enableFeedback }.values
            .filter { Packet.Message.ToHost == HostID.All || it.acceptHost == null || it.acceptHost == Packet.Message.ToHost }

        l.forEach { it.invoke(Packet.Message) }
    }

    fun getChannel(enableFeedback: Boolean = false, acceptHost: HostID? = null, description: String? = null): IChannelControl
    {
        val id = _ids++

        val lc = LocalChannel(this, id, enableFeedback, acceptHost, description)

        _lst[id] = lc

        return lc
    }
}