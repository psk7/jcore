package pvt.psk.jcore.administrator.peerCommands

enum class CommandID {

    /**
     * Требование отправки информации о хосте тому, от кого пришла эта команда
     */
    Discovery,

    /**
     * Информация о хосте, отправившем команду
     */
    HostInfo,

    /**
     * Сообщение о скором отключении хоста, отправившего команду
     */
    Leave,

    /**
     * Проверка наличия хоста в домене
     */
    Ping,

    /**
     * Ответ на запрос наличия
     */
    PingReply,

    EndOfList
}