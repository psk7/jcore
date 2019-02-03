package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.utils.BinaryWriter
import pvt.psk.jcore.utils.BinaryReader





interface IPeerCommandFactory {

    fun serialize(Command: PeerCommand, Writer: BinaryWriter)

    fun create(Reader: BinaryReader): PeerCommand?
}