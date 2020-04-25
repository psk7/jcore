package pvt.psk.jcore.host

import pvt.psk.jcore.utils.*
import java.util.*

/**
 * Уникальный идентификатор хоста
 */
class HostID {

    /**
     * Уникальный идентификатор хоста.
     * Для идентификации используется **только** значение этого поля.
     */
    val id: Int

    /**
     * Дополнительное имя хоста. Для идентификации **не используется**
     */
    val name: String?

    constructor(Reader: BinaryReader) : this(Reader.readInt32())

    constructor(ID: UUID, Name: String? = null) : this(ID.hashCode(), Name)

    constructor(ID: Int, Name: String? = null) {
        this.id = ID
        name = Name
    }

    inline val isLocal: Boolean
        get() = this == Local

    inline val isBroadcast: Boolean
        get() = this == All

    inline val isUnknown: Boolean
        get() = this == Unknown

    companion object {
        fun new(Name: String) = HostID(UUID.randomUUID(), Name)

        val Unknown = HostID(0, "Unknown")
        val Local = HostID(1, "Local")
        val All = HostID(2, "All")
    }

    /**
     * Сравнивает два идентификатор хоста. Сравнивается **только** поле ID
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HostID

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int = id

    override fun toString(): String = "<$id>"

    /**
     * Сериализация идентификатора в двоичное представление
     */
    fun serialize(writer: BinaryWriter): Unit = writer.write(id)
}