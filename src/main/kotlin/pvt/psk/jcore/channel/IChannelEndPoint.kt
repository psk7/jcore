package pvt.psk.jcore.channel

interface IChannelEndPoint {

    /**
     * Отправка сообщения в канал
     */
    fun sendMessage(message: Message)

    /**
     * Закрытие канала
     */
    fun close()
}