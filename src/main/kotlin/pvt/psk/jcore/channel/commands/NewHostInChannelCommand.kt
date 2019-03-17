package pvt.psk.jcore.channel.commands

import kotlinx.coroutines.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*

class NewHostInChannelCommand(val endPointInfo: EndPointInfo, val complete: Deferred<Unit>) : Message()