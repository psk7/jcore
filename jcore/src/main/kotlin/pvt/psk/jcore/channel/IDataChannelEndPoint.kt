package pvt.psk.jcore.channel

import kotlinx.coroutines.*
import pvt.psk.jcore.host.*
import pvt.psk.jcore.relay.*
import java.io.*

interface IDataChannelEndPoint {

    /**
     * Отправляет сообщение в канал
     */
    fun sendMessage(data:ByteArray, target: HostID? = null, metadata: Array<Any>? = null, tag: PacketTag? = null)

    /**
     * Отправляет сообщение в канал
     */
    fun sendMessage(data:ByteArray, targets: Array<HostID>, metadata: Array<Any>? = null, tag: PacketTag? = null)

    /**
     * Передает исходящий поток данных в канале
     */
    fun sendStream(target: HostID? = null, metadata: Array<Any>? = null, tag: PacketTag? = null) : Deferred<OutputStream>

    /**
     * Передает исходящий поток данных в канале
     */
    fun sendStream(targets: Array<HostID>, metadata: Array<Any>? = null, tag: PacketTag? = null) : Deferred<OutputStream>

    /**
     * Передает исходящий поток данных в канале
     */
    fun sendStream(source:InputStream, target: HostID? = null, metadata: Array<Any>? = null, tag: PacketTag? = null) : Job

    /**
     * Передает исходящий поток данных в канале
     */
    fun sendStream(source:InputStream, targets: Array<HostID>, metadata: Array<Any>? = null, tag: PacketTag? = null) : Job

    /**
     * Закрывает канал
     */
    fun close()
}