package pvt.psk.jcore.channel

import pvt.psk.jcore.utils.*

interface IChannel
{
    fun sendMessage(Data: Message)

    val received : Event<DataReceived>
}