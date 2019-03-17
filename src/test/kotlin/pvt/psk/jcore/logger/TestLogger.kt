package pvt.psk.jcore.logger

import org.joda.time.*

class TestLogger(val prefix: String = "") : Logger() {
    override fun writeLog(TimeStamp: DateTime, importance: LogImportance, logCat: String, message: String) {
        println(message)
    }
}