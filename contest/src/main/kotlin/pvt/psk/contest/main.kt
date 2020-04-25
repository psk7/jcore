package pvt.psk.contest

import org.koin.core.context.*
import pvt.psk.jcore.*
import pvt.psk.jcore.channel.*
import java.io.*
import java.nio.*

private fun createId(): String {
    var r = ""

    for (i in 0..4) {
        val next = (0..52).random()

        if (next < 26)
            r += (next + 'A'.toByte()).toChar()
        else
            r += (next - 26 + 'a'.toByte()).toChar()
    }

    return r
}

private fun printHelp() {
    println("Q - Выход")
    println("1 - Список хостов")
    println("2 - Подключение к каналу")
    println("3 - Отправка сообщения в канал")
    println("P - Печать состояния каналов")
    println("T - Печать состояния задач")
}

fun main(args: Array<String>) {
    printHelp()
    println()

    startKoin { modules(jcoreModule) }

    val l = Logger()

    val inst = Instance(createId())
    inst.init()

    var chan: IDataChannelEndPoint? = null

    l@ while (true) {
        val k = readLine()!!.toLowerCase().trim()

        if (k == "q")
            break

        when (k) {
            "h", "p", "?" -> {
                printHelp()
                println()
            }

            "1"           -> {
                println("Список известных хостов:")

                //inst.getHosts().forEach { println(it) }
            }

            "2"           -> {
                if (chan != null) {
                    println("Канал уже подключен")
                    continue@l
                }

                println("Подключение к каналу Channel2")

                chan = inst
                    .joinChannel("Channel2")
                    .getChannel(::f, null)
            }
        }
    }
}

fun f(e: Envelope, p: DataPacket) {
    when (p) {
        is BytesPacket  -> println(Charsets.US_ASCII.decode(ByteBuffer.wrap(p.data)))

        is StreamPacket -> {

            val bs = ByteArrayOutputStream()
            p.addTargetStream(bs)

            p.completed.invokeOnCompletion {
                println(Charsets.US_ASCII.decode(ByteBuffer.wrap(bs.toByteArray())))
            }
        }
    }
}
