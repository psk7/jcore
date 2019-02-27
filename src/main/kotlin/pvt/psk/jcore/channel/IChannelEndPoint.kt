package pvt.psk.jcore.channel

interface IChannelEndPoint {

    /**
     * Отправка сообщения в канал
     */
    fun sendMessage(Data: Message)

    /**
     * Закрытие канала
     */
    fun close()
}