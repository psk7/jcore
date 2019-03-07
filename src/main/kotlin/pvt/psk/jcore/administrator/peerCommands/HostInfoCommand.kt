package pvt.psk.jcore.administrator.peerCommands

import kotlinx.coroutines.*
import pvt.psk.jcore.host.*
import java.util.*

open class HostInfoCommand(val SequenceID: Int, FromHost: HostID, val endPoints: Array<EndPointInfo>, ToHost: HostID,
                           vararg val payload: Any) :
    PeerCommand(CommandID.HostInfo, FromHost, ToHost) {

    private val _ct = CompletableDeferred<Unit>()
    private val _tl = mutableListOf<Job>()
    private val _fins = mutableListOf<() -> Unit>()

    fun addTask(job: Job) = synchronized(_tl) { _tl.add(job) }

    val complete: Job = GlobalScope.launch(Dispatchers.Unconfined) {
        _ct.join()

        for (j in synchronized(_tl) { _tl.toTypedArray() })
            j.join()
    }

    fun release() {
        _ct.complete(Unit)

        runBlocking { complete.join() }

        _fins.forEach { it() }
        _fins.clear()
    }

    fun addFinalizer(f: () -> Unit) {
        synchronized(_fins) {
            _fins.add(f)
        }
    }

    override fun toString(): String = "HostInfo: From $FromHost, SeqID=$SequenceID"
}