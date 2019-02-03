package pvt.psk.jcore.administrator

import pvt.psk.jcore.administrator.peerCommands.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*

abstract class PeerCommandSocket protected constructor(val Bus: IChannel,
                                                       val Log: Logger?,
                                                       val CommandFactory: IPeerCommandFactory,
                                                       CancellationToken: CancellationToken) {

    init {
        Bus.received += { (_, p) -> onBusCmd(p) }
    }

    protected val LogCat: String = "PeerCmd"

    private fun onBusCmd(data: Message) {
        if (data !is PeerCommand || !data.ToHost.isNetwork)
            return

        Log?.writeLog(LogImportance.Trace, LogCat, "Отправка команды $data. Хосту ${data.ToHost}")

        var wr = BinaryWriter()

        CommandFactory.serialize(data, wr)

        var dg = wr.toArray()

        send(dg, data.ToHost)
    }

    protected abstract fun send(datagram: ByteArray, target: HostID)

    protected fun onReceive(Message :Message)
    {
        Log?.writeLog(LogImportance.Trace, LogCat, "Принята команда $Message для ${Message.ToHost}")

        Bus.sendMessage(Message);
    }

    abstract fun BeginReceive()
}