package pvt.psk.jcore.channel

import pvt.psk.jcore.host.*
import pvt.psk.jcore.relay.*
import java.io.*

typealias DataChannelReceived = (Envelope, DataPacket) -> Unit
typealias DataChannelBytesReceived = (Envelope, ByteArray) -> Unit
typealias DataChannelStreamReceived = (Envelope, InputStream) -> Unit
typealias NewHostInDataChannel = (HostID) -> Unit

data class Envelope(val from: HostID, val tag: PacketTag) 