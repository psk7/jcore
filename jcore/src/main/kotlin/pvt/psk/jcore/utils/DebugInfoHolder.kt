package pvt.psk.jcore.utils

import java.net.*
import kotlinx.atomicfu.*

class DebugInfoHolder {

    var broadcasts : Array<InetSocketAddress> = emptyArray()
    var remotes : Array<InetSocketAddress> = emptyArray()
    val receives = atomic(0)
    val sends = atomic(0)
    val failedReceives = atomic(0)
    val failedSends = atomic(0)
    val rebinds = atomic(0)

    var lastFailedSendMessage = ""
}