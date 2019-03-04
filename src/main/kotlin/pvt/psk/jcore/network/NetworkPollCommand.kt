package pvt.psk.jcore.network

import pvt.psk.jcore.administrator.*
import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.host.*

class NetworkPollCommand : PollCommand(HostID.Local, HostID.Local) {

    override fun createHostInfoCommand(SeqID: Int, FromHost: HostID, ToHost: HostID): HostInfoCommand {
        /*var chans = Channels.Select(kp => NetworkEndPointInfo.Create(kp.Key, ((NetworkChannel)kp.Value).LocalEndPoint.Target.Port, false,
        FromHost)).ToArray();*/

        val chans = _chans.map { create(it.key, (it.value as NetworkChannel).networkLocalEndPoint.target.port, false, FromHost, true) }.toTypedArray()

        return HostInfoCommand(SeqID, FromHost, chans, ToHost)
    }
}