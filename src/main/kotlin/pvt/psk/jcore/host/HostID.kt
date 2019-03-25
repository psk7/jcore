package pvt.psk.jcore.host

import pvt.psk.jcore.utils.*
import java.util.*

/**
 * Уникальный идентификатор хоста
 */
class HostID {

    /**
     * Уникальный идентификатор хоста.
     * Для идетификации используется **только** значение этого поля.
     */
    val ID: UUID

    /**
     * Дополнительное имя хоста. Для идентификации **не используется**
     */
    val name: String

    val isNetwork: Boolean
        get() = this != Local

    val isLocal: Boolean
        get() = this == Local

    val isBroadcast: Boolean
        get() = this == All || this == Network

    constructor(ID: UUID, Name: String) {
        this.ID = ID
        this.name = Name
    }

    constructor(Reader: BinaryReader) {
        ID = Reader.readUUID()
        name = Reader.readString()
    }

    companion object {
        /**
         * Идентифицирует **все** хосты в сети, в том числе еще неизвестные
         */
        val All: HostID = HostID(UUID.fromString("00000000-0000-0000-0000-000000000000"), "*")

        /**
         * Идентифицирует самого себя
         */
        val Local: HostID = HostID(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Local")

        /**
         * Идентифицирует **только** удаленные хосты в сети
         */
        val Network: HostID = HostID(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Network")

        val Unknown: HostID = HostID(UUID.fromString("00000000-0000-0000-0000-000000000003"), "Unknown")
    }

    /**
     * Сравнивает два идентификатор хоста. Сравнивается **только** поле ID
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HostID

        if (ID != other.ID) return false

        return true
    }

    override fun hashCode(): Int {
        return ID.hashCode()
    }

    override fun toString(): String = "$name<${ID.toString().subSequence(0, 8)}>"

    /**
     * Сериализация идентификатора в двоичное представление
     */
    fun serialize(writer: BinaryWriter) {
        writer.write(ID)
        writer.write(name)
    }
}