package com.grytsyna.fixitpro.receiver

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import android.provider.Telephony
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.activity.main.order.OrderViewModel
import com.grytsyna.fixitpro.db.DatabaseHelper
import com.grytsyna.fixitpro.entity.Order
import kotlinx.coroutines.*
import java.util.Date

class MyApplication : Application(), CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Main + job

    fun readSmsFromArchive() {
        try {
            val contentResolver: ContentResolver = contentResolver
            val smsUri: Uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )

            launch(Dispatchers.IO) {
                val orders = mutableListOf<Order>()
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
                            orders.add(newOrder)
                        }
                    }
                }

                if (orders.isNotEmpty()) {
                    val dbHelper = DatabaseHelper.getInstance(this@MyApplication)
                    val viewModel = OrderViewModel.getInstance().apply {
                        setDatabaseHelper(dbHelper)
                    }

                    orders.forEach { order ->
                        SmsReceiver.processOrder(dbHelper, viewModel, order)
                    }

                    withContext(Dispatchers.Main) {
                        viewModel.loadOrdersFromDatabase()
                    }
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
