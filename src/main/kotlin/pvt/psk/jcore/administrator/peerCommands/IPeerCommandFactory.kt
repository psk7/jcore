package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.utils.*

interface IPeerCommandFactory {

    /**
     * Сериализация команды в двоичное представление
     */
    fun serialize(Command: PeerCommand, Writer: BinaryWriter)

    /**
     * Десериализация команды из двоичного представления
     * @return Десериализованная команда, или null в случае неудачи
     */
    fun create(Reader: BinaryReader): PeerCommand?

    /**
     * Осуществляет фильтрацию команд
     * @return false если команда должна быть отброшена
     */
    fun filter(command: PeerCommand): Boolean
}