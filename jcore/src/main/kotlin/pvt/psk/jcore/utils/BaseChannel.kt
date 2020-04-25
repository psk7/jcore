package pvt.psk.jcore.utils

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.relay.*
import java.io.*

fun IDataChannel.getChannel(received: DataChannelReceived? = null, acceptTag: String? = null) =
    getChannel(received, PacketTag(acceptTag))

fun IDataChannel.getChannelBytes(received: DataChannelBytesReceived? = null, acceptTag: String? = null) =
    getChannelBytes(received, PacketTag(acceptTag))

fun IDataChannel.getChannelStream(received: DataChannelStreamReceived? = null, acceptTag: String? = null) =
    getChannelStream(received, PacketTag(acceptTag))

fun IDataChannelEndPoint.sendMessage(data: ByteArray, target: HostID? = null, metadata: Array<Any>? = null, tag: String? = null) =
    sendMessage(data, target, metadata, PacketTag(tag))

fun IDataChannelEndPoint.sendMessage(data: ByteArray, targets: Array<HostID>, metadata: Array<Any>? = null, tag: String? = null) =
    sendMessage(data, targets, metadata, PacketTag(tag))

fun IDataChannelEndPoint.sendStream(target: HostID? = null, metadata: Array<Any>? = null, tag: String? = null) =
    sendStream(target, metadata, PacketTag(tag))

fun IDataChannelEndPoint.sendStream(targets: Array<HostID>, metadata: Array<Any>? = null, tag: String? = null) =
    sendStream(targets, metadata, PacketTag(tag))

fun IDataChannelEndPoint.sendStream(source: InputStream, target: HostID? = null, metadata: Array<Any>? = null, tag: String? = null) =
    sendStream(source, target, metadata, PacketTag(tag))

fun IDataChannelEndPoint.sendStream(source: InputStream, targets: Array<HostID>, metadata: Array<Any>? = null, tag: String? = null) =
    sendStream(source, targets, metadata, PacketTag(tag))