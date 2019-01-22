package pvt.psk.jcore.administrator.peerCommands

import kotlinx.coroutines.*
import pvt.psk.jcore.host.*

class HostInfoCommand(val SequenceID: Int, FromHost: HostID, val endPoints: Array<EndPointInfo>, ToHost: HostID, vararg val payload: Array<Any>) :
    PeerCommand(CommandID.HostInfo, FromHost, ToHost) {
    val _ct = CompletableDeferred<Unit>()
    val _tl = mutableListOf<Job>()

    fun addTask(job: Job) = synchronized(_tl) { _tl.add(job) }

    fun complete(): Job = GlobalScope.async {
        _ct.join()

        for (j in synchronized(_tl) { _tl.toTypedArray() })
            j.join()
    }

    fun release() {
        _ct.complete(Unit)

        runBlocking { complete().join() }
    }
}