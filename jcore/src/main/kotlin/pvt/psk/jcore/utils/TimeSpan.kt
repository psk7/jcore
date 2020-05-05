package pvt.psk.jcore.utils

import java.util.*

data class TimeSpan(val milliseconds: Long) {
    operator fun plus(other: TimeSpan) = TimeSpan(milliseconds + other.milliseconds)
    operator fun plus(other: Date) = Date(milliseconds + other.getTime())
    fun and(other: TimeSpan): TimeSpan = this + other
    fun from(date: Date): Date = this + date
}

val Int.milliseconds : TimeSpan get() = TimeSpan(this.toLong())

val Int.seconds: TimeSpan get() = TimeSpan(this * 1000L)
val Int.second: TimeSpan get() = this.seconds

val Int.minutes: TimeSpan get() = TimeSpan(this * 60000L)
val Int.minute: TimeSpan get() = this.minutes
