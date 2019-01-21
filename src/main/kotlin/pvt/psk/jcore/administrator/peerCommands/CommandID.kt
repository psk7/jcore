package pvt.psk.jcore.administrator.peerCommands

enum class CommandID
{
    /// <summary>
    /// Требование отправки информации о хосте тому, от кого пришла эта команда
    /// </summary>
    Discovery,

    /// <summary>
    /// Информация о хосте, отправившем команду
    /// </summary>
    HostInfo,

    /// <summary>
    /// Сообщение о скором отключении хоста, отправившего команду
    /// </summary>
    Leave,

    /// <summary>
    /// Проверка наличия хоста в домене
    /// </summary>
    Ping,

    /// <summary>
    /// Ответ на запрос наличия
    /// </summary>
    PingReply,

    EndOfList
}