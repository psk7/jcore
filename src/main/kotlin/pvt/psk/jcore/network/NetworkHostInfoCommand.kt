package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*
import java.net.*

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

fun HostInfoCommand.getSourceIPAddress(): Deferred<InetAddress> = (payload[0] as CompletableDeferred<InetAddress>)

fun HostInfoCommand.setSourceIpAddress(Address: InetAddress) {
    (payload[0] as CompletableDeferred<InetAddress>).complete(Address)
}

fun BinaryReader.deserialize(fromHost: HostID): Array<EndPointInfo> {

    // Версия упаковки
    val unused = readByte()

    val canrcvstream = readBoolean()

    return Array(readInt16().toInt()) {
        create(ReadString(), readInt32(), readBoolean(), fromHost, canrcvstream)
    }
}

fun HostInfoCommand.create(SequenceID: Int, From: HostID, endPoints: Array<EndPointInfo>, SourceIPAddress: InetAddress, To: HostID): HostInfoCommand =
    HostInfoCommand(SequenceID, From, endPoints, To, CompletableDeferred(SourceIPAddress)).also {
        setSourceIpAddress(SourceIPAddress)
    }

