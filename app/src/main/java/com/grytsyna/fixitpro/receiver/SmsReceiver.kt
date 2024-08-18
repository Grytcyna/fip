package com.grytsyna.fixitpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.activity.main.order.OrderViewModel
import com.grytsyna.fixitpro.db.DatabaseHelper
import com.grytsyna.fixitpro.entity.Order
import com.grytsyna.fixitpro.enum_status.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                val fullMessage = StringBuilder()
                var timestampMillis: Long = 0

                for (message in messages) {
                    val messageBody = message.messageBody
                    timestampMillis = message.timestampMillis
                    fullMessage.append(messageBody)
                }

                val order = SmsParser.parseMessage(fullMessage.toString())
                if (order != null) {
                    val finalOrder = order.toBuilder().receivedDate(Date(timestampMillis)).build()
                    processOrderInBackground(context, finalOrder)
                }
            } else {
                LogWrapper.d("SmsReceiver: Received unexpected action: ${intent.action}")
            }
        } catch (t: Throwable) {
            LogWrapper.e("SmsReceiver: onReceive", t.message ?: "Unknown error", t)
        }
    }

    private fun processOrderInBackground(context: Context, order: Order) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dbHelper = DatabaseHelper.getInstance(context)
                val viewModel = OrderViewModel.getInstance()
                viewModel.setDatabaseHelper(dbHelper)

                processOrder(dbHelper, viewModel, order)
            } catch (t: Throwable) {
                LogWrapper.e("SmsReceiver: processOrderInBackground", t.message ?: "Unknown error", t)
            }
        }
    }

    companion object {
        suspend fun processOrder(dbHelper: DatabaseHelper, viewModel: OrderViewModel, order: Order) {
            try {
                synchronized(dbHelper) {
                    val existOrder = dbHelper.getOrderById(order.id)
                    if (existOrder != null) {
                        if (existOrder.status == Status.DELETED) {
                            return
                        }

                        if (order.receivedDate.after(existOrder.receivedDate)) {
                            val builder = existOrder.toBuilder()
                                .date(order.date)
                                .receivedDate(order.receivedDate)
                                .note(order.note)
                                .masterTime(order.masterTime)
                                .address(order.address)
                                .tel(order.tel)
                                .surplus(order.surplus)
                                .serviceFee(order.serviceFee)

                            if (existOrder.status == Status.NEW) {
                                builder.status(order.status)
                            }

                            val finalOrder = builder.build()
                            dbHelper.updateParsedOrder(finalOrder)
                        }
                    } else {
                        dbHelper.addOrder(order)
                    }
                }

                withContext(Dispatchers.Main) {
                    viewModel.loadOrdersFromDatabase()
                }
            } catch (t: Throwable) {
                LogWrapper.e("SmsReceiver: processOrder", t.message ?: "Unknown error", t)
            }
        }
    }
}
