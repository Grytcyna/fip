package com.grytsyna.fixitpro.common

import android.widget.EditText
import com.grytsyna.fixitpro.common.Constants.DATE_FORMATTER
import java.util.Calendar
import java.util.Date

object DateUtils {

    fun getStartDateOrToday(editText: EditText): Date {
        return getDateOrToday(editText, true)
    }

    fun getEndDateOrToday(editText: EditText): Date {
        return getDateOrToday(editText, false)
    }

    private fun getDateOrToday(editText: EditText, isStartOfDay: Boolean): Date {
        val dateStr = editText.text.toString()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = runCatching {
                DATE_FORMATTER.parse(dateStr)?.time ?: timeInMillis
            }.getOrElse {
                timeInMillis
            }

            if (isStartOfDay) {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 1)
            } else {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
        }
        return calendar.time
    }

    fun getStartDateOrToday(calendar: Calendar): Date {
        return getDateOrToday(calendar, true)
    }

    fun getEndDateOrToday(calendar: Calendar): Date {
        return getDateOrToday(calendar, false)
    }

    private fun getDateOrToday(calendar: Calendar, isStartOfDay: Boolean): Date {
        if (isStartOfDay) {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 1)
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
        }
        return calendar.time
    }

    fun getFirstDayOfCurrentMonth(): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return getStartDateOrToday(calendar)
    }

    fun getEighthDayOfNextMonth(): Date {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 8)
        }
        return getEndDateOrToday(calendar)
    }
}
