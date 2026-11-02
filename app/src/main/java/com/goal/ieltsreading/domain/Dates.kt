package com.goal.ieltsreading.domain

import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object Dates {
    /** Days since 1970-01-01 in the phone's time zone. */
    fun today(): Long = epochDay(System.currentTimeMillis())

    fun epochDay(millis: Long): Long {
        val offset = TimeZone.getDefault().getOffset(millis)
        return TimeUnit.MILLISECONDS.toDays(millis + offset)
    }

    /** Short weekday name such as "Mon" for an epoch day. */
    fun weekdayShort(epochDay: Long): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = TimeUnit.DAYS.toMillis(epochDay)
        return arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }
}
