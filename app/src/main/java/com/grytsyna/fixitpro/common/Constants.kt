package com.grytsyna.fixitpro.common

import java.text.SimpleDateFormat
import java.util.Locale

object Constants {
    const val DATE_FORMAT = "MM/dd/yyyy"
    val DATE_FORMATTER = SimpleDateFormat(DATE_FORMAT, Locale.US)

    val SMS_PATTERN_REGEX = "#\\d+; \\d{2}/\\d{2}/\\d{4}.* \\d{1,2}(AM|PM)\\s*-\\s*\\d{1,2}(AM|PM)".toRegex()
    val MASTER_TIME_REGEX = "\\s".toRegex()
    val FEE_REGEX = """\$(\d+(\.\d{1,2})?)""".toRegex()

    const val HASH = "#"
    const val SEMICOLON = ";"
    const val SPASE = " "
    const val COLON = ":"
    const val ADDR = "addr:"
    const val TEL = "tel:"
    const val SLASH = "/"
    const val EMPTY = ""

    val TIME_ORDER = arrayOf(
        "12AM", "1AM", "2AM", "3AM", "4AM", "5AM", "6AM", "7AM", "8AM", "9AM", "10AM", "11AM",
        "12PM", "1PM", "2PM", "3PM", "4PM", "5PM", "6PM", "7PM", "8PM", "9PM", "10PM", "11PM"
    )

    const val REQUEST_CODE_CREATE_FILE = 1
    const val REQUEST_CODE_OPEN_FILE = 2
    const val REQUEST_CODE_IMPORT_DATA = 3
    const val REQUEST_CODE_PERMISSIONS = 101

    const val EXTRA_TEL = "tel:"
    const val EXTRA_GEO = "geo:0,0?q="

    const val EXTRA_ORDER = "order"
    const val EXTRA_IMPORT_RESULT = "import_result"
}
