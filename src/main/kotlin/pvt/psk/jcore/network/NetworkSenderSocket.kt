package pvt.psk.jcore.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.logger.*
import pvt.psk.jcore.utils.*
import java.net.*

class NetworkSenderSocket(private val udp: SafeUdpClient, cancellationToken: CancellationToken,
                          private val list: IPEndPointsList, logger: Logger?) :
        SenderSocket(cancellationToken, logger) {

    enum class PacketID(val id: Int) {

        Datagram(1),
        Stream(2),
        Ping(3),
        PingReply(4),
    }

    private val fmt = Formatter()

    val tcp = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().bind(InetSocketAddress("::", 0))

    init {
        GlobalScope.launch { beginAccept() }

        udp.received += { b, from -> udpReceived(b, from) }
    }

    private suspend fun beginAccept() {

        val s = tcp.accept()

        beginAccept()

        val rc = s.openReadChannel()

        try {
            val t = AckToken(BinaryReader(rc))

            t.received(rc)
        } catch (e: Exception) {
            return
        }
    }

    private fun udpReceived(bytes: ByteArray, from: InetSocketAddress) {

        val rd = BinaryReader(bytes)

        val b = rd.readByte()

        val pid = PacketID.values().find { it.id == b.toInt() }

        when (pid) {
            PacketID.Datagram -> receiveDatagram(rd, from)
            PacketID.Ping -> {
            }
        }
    }

    private fun receiveDatagram(reader: BinaryReader, from: InetSocketAddress) = GlobalScope.launch(
            Dispatchers.Unconfined) {

        var ep: EndPoint = list.find(from).await()

        val mtd = fmt.deserialize(reader)
        var d  = fmt.deserialize(reader) as ByteArray

        ep.onReceived(BytesPacket(d, ep.targetHost, HostID.Local))
    }

    /**
     * Передача датаграммы удаленному хосту
     * @param Data Передаваемые данные
     * @param Target Адрес точки приема удаленного хоста
     */
    override fun sendDatagram(data: BytesPacket, Target: EndPoint) {

        if (Target !is NetworkEndPoint)
            return

        val d = data.Data
        val tgt = Target.target

        val wr = BinaryWriter()

        wr.write(PacketID.Datagram.id.toByte()) // Маркер байтовой посылки

        fmt.serialize(wr, data.Metadata) // Метаданные
        fmt.serialize(wr, d)             // Данные

        udp.send(wr.toArray(), tgt!!)
    }

    override fun safeClose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
