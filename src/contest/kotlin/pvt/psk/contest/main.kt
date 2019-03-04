package pvt.psk.contest

import kotlinx.coroutines.*
import pvt.psk.jcore.channel.*
import pvt.psk.jcore.logger.*
import java.io.Console

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

    val l = Logger()

    val inst = Instance(createId())
    inst.init()

    var chan : IChannelEndPoint? = null

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

                inst.getHosts().forEach { println(it) }
            }

            "2"           -> {
                if (chan == null)
                {
                    println("Канал уже подключен")
                    break@l
                }

                println("Подключение к каналу Channel2")

                chan = inst.joinChannel("Channel").getChannel()
            }
        }
    }
}
