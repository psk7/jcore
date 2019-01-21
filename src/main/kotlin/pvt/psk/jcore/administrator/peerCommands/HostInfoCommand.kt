package pvt.psk.jcore.administrator.peerCommands

import kotlinx.coroutines.*
import pvt.psk.jcore.host.*

class HostInfoCommand(val SequenceID: Int, FromHost: HostID, val endPoints: Array<EndPointInfo>, ToHost: HostID, vararg val payload: Array<Any>) :
    PeerCommand(CommandID.HostInfo, FromHost, ToHost)
{
    val _ct = CompletableDeferred<Unit>()

    fun addTask(job: Job)
    {
    }

    fun complete(): Job = _ct

    fun release()
    {

    }
}