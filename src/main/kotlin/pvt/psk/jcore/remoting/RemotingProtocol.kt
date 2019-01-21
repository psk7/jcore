package pvt.psk.jcore.remoting

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*

class RemotingProtocol(val remotingChannel: IChannel, val selfHostID: HostID, val factory: IMethodProxyFactory, log: Logger?) {

    data class MethodInvokerBag(val invoker: IMethodInvoker, val isLocal: Boolean)

    private enum class PacketID {
        Invoke,
        Result
    }

    fun createRequest(token: AckToken, taskID: TaskID, methodID: MethodID, arguments: Arguments): ByteArray {
        val ms = MemoryStream()
        val wr = BinaryWriter(ms)

        wr.Write(PacketID.Invoke as Byte)
        token.ToStream(ms)
        taskID.Serialize(wr)
        methodID.Serialize(wr)
        //Arguments.Formatter.Serialize(ms, Arguments)

        return ByteArray(0)
    }

}