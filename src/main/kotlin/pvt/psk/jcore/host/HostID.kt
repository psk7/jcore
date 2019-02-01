package pvt.psk.jcore.host

import pvt.psk.jcore.utils.*
import java.util.*

class HostID {
    val ID: UUID
    val Name: String

    val isNetwork: Boolean
        get() = this != Local

    val isLocal: Boolean
        get() = this == Local

    constructor(ID: UUID, Name: String) {
        this.ID = ID
        this.Name = Name
    }

    constructor(Reader: BinaryReader) {
        ID = Reader.ReadUUID()
        Name = Reader.ReadString()
    }

    companion object {
        val All: HostID = HostID(UUID.fromString("00000000-0000-0000-0000-000000000000"), "*")
        val Local: HostID = HostID(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Local")
        val Network: HostID = HostID(UUID.fromString("00000000-0000-0000-0000-000000000002"), "Network")
        val Unknown: HostID = HostID(UUID.fromString("00000000-0000-0000-0000-000000000003"), "Unknown")
    }

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

    override fun toString(): String = "$Name<${ID.toString().subSequence(0, 8)}>"
}