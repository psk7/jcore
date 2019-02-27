package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*
import java.net.*

fun HostInfoCommand.serialize(writer: BinaryWriter) {

    writer.write(endPoints.size.toShort())

    for (e in endPoints)
        e.run {
            writer.write(channelName)
            writer.write(port)
            writer.write(readOnly)
        }
}

fun HostInfoCommand.getSourceIPAddress() : Deferred<InetAddress>{
    return CompletableDeferred<InetAddress>()
}

fun HostInfoCommand.setSourceIpAddress(Address: InetAddress) {

}

fun BinaryReader.deserialize(fromHost: HostID): Array<EndPointInfo> {
    return arrayOf()
}