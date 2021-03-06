package pvt.psk.contest

import org.joda.time.*
import pvt.psk.jcore.logger.*
import java.time.LocalDateTime
import java.time.format.*

fun String.red() = "\u001b[31m${this}\u001b[0m"
fun String.yellow() = "\u001b[33m${this}\u001b[0m"
fun String.gray() = "\u001b[30;1m${this}\u001b[0m"
fun String.white() = "\u001b[37m${this}\u001b[0m"

fun String.format(timeStamp: DateTime, importance: LogImportance, logCat: String) =
    "[${timeStamp.toString("HH:mm:ss")}] [$importance]: <$logCat> $this"

open class Logger : pvt.psk.jcore.logger.Logger() {
    override fun writeLog(TimeStamp: DateTime, importance: LogImportance, logCat: String, message: String) {

        when (importance) {
            LogImportance.Trace   -> println(message.format(TimeStamp, importance, logCat).gray())
            LogImportance.Info    -> println(message.format(TimeStamp, importance, logCat).white())
            LogImportance.Warning -> println(message.format(TimeStamp, importance, logCat).yellow())
            LogImportance.Error   -> println(message.format(TimeStamp, importance, logCat).red())
        }
    }
}