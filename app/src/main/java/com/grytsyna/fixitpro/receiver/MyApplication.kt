package com.grytsyna.fixitpro.receiver

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import com.grytsyna.fixitpro.activity.main.OnOrdersLoadedListener
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.common.DateUtils
import com.grytsyna.fixitpro.db.DatabaseHelper
import kotlinx.coroutines.*
import java.util.Date

class MyApplication : Application(), CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Main + job

    private var ordersLoadedListener: OnOrdersLoadedListener? = null

    fun setOrdersLoadedListener(listener: OnOrdersLoadedListener) {
        ordersLoadedListener = listener
    }

    fun removeOrdersLoadedListener() {
        ordersLoadedListener = null
    }

    fun readSmsFromArchive() {
        try {
            val dbHelper = DatabaseHelper.getInstance(this@MyApplication)
            val contentResolver: ContentResolver = contentResolver
            val smsUri: Uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )

            launch(Dispatchers.IO) {
                contentResolver.query(
                    smsUri,
                    projection,
                    null,
                    null,
                    Telephony.Sms.DEFAULT_SORT_ORDER
                )?.use { cursor ->
                    val indexBody = cursor.getColumnIndex(Telephony.Sms.BODY)
                    val indexDate = cursor.getColumnIndex(Telephony.Sms.DATE)
                    while (cursor.moveToNext()) {
                        val body = cursor.getString(indexBody)
                        val date = cursor.getLong(indexDate)

                        val order = SmsParser.parseMessage(body)
                        if (order != null) {
                            val newOrder = order.toBuilder()
                                .receivedDate(Date(date))
                                .build()

                            SmsReceiver.processOrder(dbHelper, newOrder)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    ordersLoadedListener?.onOrdersLoaded(dbHelper.getRangeOrders(
                        DateUtils.getFirstDayOfCurrentMonth(),
                        DateUtils.getEighthDayOfNextMonth())
                    )
                }
            }
        } catch (t: Throwable) {
            LogWrapper.e("MyApplication: readSmsFromArchive", t.message ?: "Unknown error", t)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        job.cancel()
    }
}
