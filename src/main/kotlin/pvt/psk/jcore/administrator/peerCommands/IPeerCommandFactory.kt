package pvt.psk.jcore.administrator.peerCommands

import pvt.psk.jcore.utils.BinaryWriter



interface IPeerCommandFactory {

    fun serialize(Command: PeerCommand, Writer: BinaryWriter)
}