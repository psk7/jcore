package pvt.psk.contest

import pvt.psk.jcore.logger.*
import java.time.*
import java.time.format.*

fun String.red() = "\u001b[31m${this}\u001b[0m"
fun String.yellow() = "\u001b[33m${this}\u001b[0m"
fun String.gray() = "\u001b[30;1m${this}\u001b[0m"
fun String.white() = "\u001b[37m${this}\u001b[0m"

fun String.format(timeStamp: LocalDateTime, importance: LogImportance, logCat: String) =
    "[${DateTimeFormatter.ofPattern("HH:mm:ss").format(timeStamp)}] [$importance]: <$logCat> $this"

open class Logger : pvt.psk.jcore.logger.Logger() {
    override fun writeLog(TimeStamp: LocalDateTime, importance: LogImportance, logCat: String, message: String) {

        when (importance) {
            LogImportance.Trace   -> println(message.format(TimeStamp, importance, logCat).gray())
            LogImportance.Info    -> println(message.format(TimeStamp, importance, logCat).white())
            LogImportance.Warning -> println(message.format(TimeStamp, importance, logCat).yellow())
            LogImportance.Error   -> println(message.format(TimeStamp, importance, logCat).red())
        }
    }
}