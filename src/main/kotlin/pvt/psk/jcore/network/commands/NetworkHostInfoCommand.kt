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
            writer.write(readOnly)
        }
}

fun BinaryReader.deserialize(fromHost: HostID): Array<EndPointInfo> {

    // Версия упаковки
    @Suppress("UNUSED_VARIABLE")
    val unused = readByte()

    val canrcvstream = readBoolean()

    return Array(readInt16().toInt()) {
        create(ReadString(), readInt32(), readBoolean(), fromHost, canrcvstream)
    }
}
