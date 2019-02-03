package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.utils.*
import java.net.*

fun HostInfoCommand.serialize(writer:BinaryWriter){

}

fun HostInfoCommand.setSourceIpAddress(Address:InetAddress){

}

fun BinaryReader.deserialize(fromHost:HostID) : Array<EndPointInfo>{
    return arrayOf()
}