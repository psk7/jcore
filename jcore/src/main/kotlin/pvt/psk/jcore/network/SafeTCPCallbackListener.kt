package pvt.psk.jcore.network

import kotlinx.coroutines.*
import pvt.psk.jcore.utils.*
import java.io.*
import java.net.*

class SafeTCPCallbackListener(endPoint: InetSocketAddress) : Closeable {

    private val listenJob: Job

    private val tcp = ServerSocket(endPoint.port)

    val localEndPoint: InetSocketAddress
        get() = tcp.localSocketAddress as InetSocketAddress

    constructor(address: InetAddress, port: Int) : this(InetSocketAddress(address, port))

    init {
        listenJob = listenTCP()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun listenTCP() = GlobalScope.launch(Dispatchers.IO) {
        while (isActive) {
            val cl =
                try {
                    tcp.accept()
                }
                catch (ignored: SocketException) {
                    delay(10); null
                } ?: continue

            launch {
                val ns = NetworkInputStream(cl.getInputStream())
                val r = BinaryReader(ns)
                val ws = NetworkOutputStream(cl.getOutputStream(), cl::close)

                AckToken(r).received(ns to ws)
            }
        }
    }

    override fun close() {
        listenJob.cancel()

        try {
            tcp.close()     // tcp.accept бросит SocketException
        }
        catch (ignore: java.lang.Exception) {
        }

        runBlocking { listenJob.join() }
    }
}