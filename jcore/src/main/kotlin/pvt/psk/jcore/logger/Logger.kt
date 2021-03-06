package pvt.psk.jcore.logger

import org.joda.time.*

/**
 * Базовый класс журнала
 */
abstract class Logger {

    fun writeLog(importance: LogImportance, logCat: String, message: String) {

        writeLog(DateTime.now(), importance, logCat, message)
    }

    protected abstract fun writeLog(TimeStamp: DateTime, importance: LogImportance, logCat: String, message: String)
}