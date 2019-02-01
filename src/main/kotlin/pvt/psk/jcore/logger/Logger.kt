package pvt.psk.jcore.logger

import java.time.*

open abstract class Logger {
    fun writeLog(importance: LogImportance, logCat: String, message: String) {

        writeLog(LocalDateTime.now(), importance, logCat, message)
    }

    protected abstract fun writeLog(TimeStamp: LocalDateTime, importance: LogImportance, logCat: String, message: String)
}