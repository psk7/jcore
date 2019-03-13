package pvt.psk.jcore.utils

import pvt.psk.jcore.administrator.peerCommands.*

fun PeerCommand.toBytes(factory: IPeerCommandFactory): ByteArray {
    val wr = BinaryWriter()

    factory.serialize(this, wr)

    return wr.toArray()
}