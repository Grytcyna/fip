package com.grytsyna.fixitpro.receiver

import com.grytsyna.fixitpro.common.Constants.ADDR
import com.grytsyna.fixitpro.common.Constants.COLON
import com.grytsyna.fixitpro.common.Constants.EMPTY
import com.grytsyna.fixitpro.common.Constants.FEE_REGEX
import com.grytsyna.fixitpro.common.Constants.HASH
import com.grytsyna.fixitpro.common.Constants.MASTER_TIME_REGEX
import com.grytsyna.fixitpro.common.Constants.SEMICOLON
import com.grytsyna.fixitpro.common.Constants.SLASH
import com.grytsyna.fixitpro.common.Constants.SMS_PATTERN_REGEX
import com.grytsyna.fixitpro.common.Constants.SPASE
import com.grytsyna.fixitpro.common.Constants.TEL
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.entity.Order
import com.grytsyna.fixitpro.enum_status.RequestStatus
import com.grytsyna.fixitpro.enum_status.Status
import java.util.Calendar
import java.util.Date

object SmsParser {

    fun parseMessage(message: String): Order? {
        return try {
            if (!SMS_PATTERN_REGEX.containsMatchIn(message)) {
                return null
            }

            var msg = message.trim()
            var parts = msg.split(HASH, limit = 2)
            val note = parts[0].trim()
            msg = parts[1].trim()
            parts = msg.split(SEMICOLON, limit = 2)
            val id = parts[0].trim().toInt()
            msg = parts[1].trim()
            parts = msg.split(SPASE, limit = 2)
            val dateString = parts[0].trim()
            msg = parts[1].trim()
            parts = msg.split(COLON, limit = 2)
            val requestStatusDescription = parts[0].trim()
            msg = parts[1].trim()
            parts = msg.split(ADDR, limit = 2)
            val masterTime = parts[0].replace(MASTER_TIME_REGEX, EMPTY).trim()
            msg = parts[1].trim()
            parts = msg.split(SEMICOLON, limit = 2)
            val address = parts[0].trim()
            msg = parts[1].trim()
            parts = msg.split(TEL, limit = 2)
            val tel = parts[1].trim().substring(0, 10)
            val surplus = parts[1].trim().substring(10)

            val extractedValue = FEE_REGEX.find(surplus)?.groups?.get(1)?.value
            val serviceFee = extractedValue?.toDoubleOrNull() ?: 99.95

            val dateParts = dateString.split(SLASH)
            val month = dateParts[0].toInt() - 1
            val day = dateParts[1].toInt()
            val year = dateParts[2].toInt()

            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)

            val requestStatus = RequestStatus.fromDescription(requestStatusDescription)
            val status = when (requestStatus) {
                RequestStatus.REQUEST_NEW_ORDER,
                RequestStatus.REQUEST_ORDER,
                RequestStatus.REQUEST_CHANGED_ORDER,
                RequestStatus.REQUEST_NEW_RECALL -> Status.NEW

                RequestStatus.REQUEST_CANCELED_ORDER -> Status.CANCELED
                else -> Status.DELETED
            }

            Order.Builder()
                .id(id)
                .status(status)
                .date(calendar.time)
                .receivedDate(Date())
                .note(note)
                .masterTime(masterTime)
                .address(address)
                .tel(tel)
                .surplus(surplus)
                .comment(EMPTY)
                .serviceFee(serviceFee)
                .partsFee(0.0)
                .build()
        } catch (t: Throwable) {
            LogWrapper.e("SmsParser: parseMessage", t.message ?: "Unknown error", t)
            null
        }
    }
}
