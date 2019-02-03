package pvt.psk.jcore.remoting

import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*

class RemotingProtocol(val remotingChannel: IChannel, val selfHostID: HostID, log: Logger?) {

    data class MethodInvokerBag(val invoker: IMethodInvoker, val isLocal: Boolean)

    private enum class PacketID {
        Invoke,
        Result
    }

    fun createRequest(token: AckToken, taskID: TaskID, methodID: MethodID, arguments: Arguments): ByteArray {
        val wr = BinaryWriter()

        wr.write(PacketID.Invoke.ordinal.toByte())
        token.toStream(wr.baseStream)
        taskID.Serialize(wr)
        methodID.Serialize(wr)
        //Arguments.Formatter.Serialize(ms, Arguments)

        return ByteArray(0)
    }

}