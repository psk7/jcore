package pvt.psk.jcore.channel

import pvt.psk.jcore.administrator.peerCommands.*

fun IChannel.sendHostInfo(command: HostInfoCommand) {
    sendMessage(command)
    command.release()
}