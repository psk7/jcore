package pvt.psk.jcore.channel

import kotlinx.coroutines.*
import pvt.psk.jcore.utils.*
import java.io.*

class StreamPacket : DataPacket {

    private val fs = ForkStream()

    val stream: OutputStream
        get() = fs

    var sourceStream: InputStream? = null

    val completed: Job
        get() = fs.transferCompleted

    constructor() : super()

    constructor(metadata: Array<Any>?) : super(metadata)

    fun addTargetStream(stream: Deferred<OutputStream?>, leaveOpen: Boolean = false): Job {
        fs.addTargetStream(stream, leaveOpen)

        return fs.transferEnabled
    }

    fun addTargetStream(stream: OutputStream, leaveOpen: Boolean = false): Job =
        addTargetStream(CompletableDeferred(stream), leaveOpen)

    fun enableTransferAsync(): Job = fs.enableTransferAsync()

    /**
     * Асинхронное завершение приема потоковой посылки
     */
    suspend fun completeStreamAsync() {

        enableTransferAsync().join()

        val ss = sourceStream!!

        try {
            /*val bc = */ss.copyTo(stream)

            //println("------------- bytes copied ${bc}")

            ss.close()
        }
        catch (e: Exception) {
            ss.close()
        }
    }

    fun dispose() {
        fs.close()
    }
}