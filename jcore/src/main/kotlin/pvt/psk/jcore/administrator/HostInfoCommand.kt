package pvt.psk.jcore.administrator

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.relay.*
import pvt.psk.jcore.utils.*

class HostInfoCommand : Message, ISerializable {

    val domain: String
    val endPoints: Array<EndPointInfo>
    val sequenceId: Int

    constructor(domain: String, endPoints: Array<EndPointInfo>, sequenceId: Int) : super() {
        this.domain = domain
        this.endPoints = endPoints
        this.sequenceId = sequenceId
    }

    constructor(reader: BinaryReader) {

        // Версия упаковки
        val unused = reader.readByte()

        sequenceId = reader.readInt32()

        domain = reader.readString()

        endPoints = Array(reader.readUInt16().toInt()) {
            EndPointInfo(HostEndpointID(reader), reader.readString(), Array(reader.readByte()) { PacketTag(reader) })
        }
    }

    override fun serialize(writer: BinaryWriter) {

        // Версия упаковки
        writer.write(1.toByte())

        writer.write(sequenceId)
        writer.write(domain)
        writer.write(endPoints.size.toUShort())

        endPoints.forEach {
            writer.write(it.target)
            writer.write(it.channelName)

            val tl = it.acceptTags.filter { x -> !x.isEmpty }.toTypedArray()

            writer.write(tl.size.toByte())

            tl.forEach(writer::write)
        }
    }
}