package pvt.psk.jcore.network.commands

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.network.*
import pvt.psk.jcore.utils.*

fun HostInfoCommand.serialize(writer: BinaryWriter) {

    // Версия упаковки
    writer.write(1.toByte())

    writer.write(false) // canReceiveStream

    writer.write(endPoints.size.toShort())

    for (e in endPoints)
        e.run {
            writer.write(channelName)
            writer.write(port)
            writer.write(acceptTags.joinToString(1.toChar().toString()))
        }
}

private fun readTags(reader:BinaryReader) =
    reader.readString().split(1.toChar()).filter { it.isNotBlank() }.toTypedArray()

fun BinaryReader.deserialize(fromHost: HostID): Array<EndPointInfo> {

    // Версия упаковки
    @Suppress("UNUSED_VARIABLE")
    val unused = readByte()

    val canrcvstream = readBoolean()

    return Array(readInt16().toInt()) {
        create(readString(), readInt32(), readTags(this), fromHost, canrcvstream)
    }
}
