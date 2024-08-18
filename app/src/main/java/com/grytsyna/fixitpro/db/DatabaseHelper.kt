package com.grytsyna.fixitpro.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.grytsyna.fixitpro.common.Constants.DATE_FORMATTER
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.entity.Order
import com.grytsyna.fixitpro.enum_status.Status
import java.util.*
import kotlin.collections.ArrayList

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private var db: SQLiteDatabase? = null

    init {
        db = writableDatabase
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            val createOrdersTable = """
                CREATE TABLE $TABLE_ORDERS (
                $COLUMN_ID INTEGER PRIMARY KEY,
                $COLUMN_STATUS TEXT,
                $COLUMN_DATE INTEGER,
                $COLUMN_RECEIVED_DATE INTEGER,
                $COLUMN_NOTE TEXT,
                $COLUMN_MASTER_TIME TEXT,
                $COLUMN_ADDRESS TEXT,
                $COLUMN_TEL TEXT,
                $COLUMN_SURPLUS TEXT,
                $COLUMN_COMMENT TEXT,
                $COLUMN_SERVICE_FEE REAL,
                $COLUMN_PARTS_FEE REAL,
                $COLUMN_SEARCH TEXT
                )
            """.trimIndent()
            db.execSQL(createOrdersTable)
            db.execSQL("CREATE INDEX idx_search ON $TABLE_ORDERS($COLUMN_SEARCH)")
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: onCreate", t.message ?: "Unknown error", t)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ORDERS")
            onCreate(db)
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: onUpgrade", t.message ?: "Unknown error", t)
        }
    }

    fun addOrder(order: Order) {
        try {
            if (!isValidId(order.id) || orderExists(order.id)) {
                LogWrapper.e("DatabaseHelper: addOrder", "Invalid or duplicate ID: ${order.id}")
                return
            }

            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_ID, order.id)
                put(COLUMN_STATUS, order.status.name)
                put(COLUMN_DATE, insertingDate(order.date.time))
                put(COLUMN_RECEIVED_DATE, order.receivedDate.time)
                put(COLUMN_NOTE, order.note)
                put(COLUMN_MASTER_TIME, order.masterTime)
                put(COLUMN_ADDRESS, order.address)
                put(COLUMN_TEL, order.tel)
                put(COLUMN_SURPLUS, order.surplus)
                put(COLUMN_COMMENT, order.comment)
                put(COLUMN_SERVICE_FEE, order.serviceFee)
                put(COLUMN_PARTS_FEE, order.partsFee)
                put(COLUMN_SEARCH, buildSearchContent(order))
            }

            db.insert(TABLE_ORDERS, null, values)
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: addOrder", t.message ?: "Unknown error", t)
        }
    }

    private fun isValidId(id: Int) = id > 0

    private fun orderExists(id: Int): Boolean {
        var cursor: Cursor? = null
        return try {
            val db = readableDatabase
            cursor = db.query(
                TABLE_ORDERS,
                arrayOf(COLUMN_ID),
                "$COLUMN_ID=?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            cursor.count > 0
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: orderExists", t.message ?: "Unknown error", t)
            false
        } finally {
            cursor?.close()
        }
    }

    fun getOrderById(id: Int): Order? {
        var cursor: Cursor? = null
        return try {
            val db = readableDatabase
            cursor = db.query(
                TABLE_ORDERS,
                null,
                "$COLUMN_ID=?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                Order.Builder()
                    .id(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                    .status(Status.fromName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))))
                    .date(Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE))))
                    .receivedDate(Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECEIVED_DATE))))
                    .note(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)))
                    .masterTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_TIME)))
                    .address(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)))
                    .tel(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEL)))
                    .surplus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SURPLUS)))
                    .comment(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT)))
                    .serviceFee(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SERVICE_FEE)))
                    .partsFee(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PARTS_FEE)))
                    .build()
            } else null
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: getOrderById", t.message ?: "Unknown error", t)
            null
        } finally {
            cursor?.close()
        }
    }

    fun getAllOrders(): List<Order> {
        val orderList = ArrayList<Order>()
        var cursor: Cursor? = null
        return try {
            val db = readableDatabase
            cursor = db.query(TABLE_ORDERS, null, null, null, null, null, null)
            if (cursor.moveToFirst()) {
                do {
                    val order = Order.Builder()
                        .id(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                        .status(Status.fromName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))))
                        .date(Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE))))
                        .receivedDate(Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECEIVED_DATE))))
                        .note(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)))
                        .masterTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_TIME)))
                        .address(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)))
                        .tel(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEL)))
                        .surplus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SURPLUS)))
                        .comment(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT)))
                        .serviceFee(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SERVICE_FEE)))
                        .partsFee(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PARTS_FEE)))
                        .build()
                    orderList.add(order)
                } while (cursor.moveToNext())
            }
            orderList
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: getAllOrders", t.message ?: "Unknown error", t)
            emptyList()
        } finally {
            cursor?.close()
        }
    }

    fun updateOrder(order: Order) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_STATUS, order.status.name)
                put(COLUMN_DATE, insertingDate(order.date.time))
                put(COLUMN_NOTE, order.note)
                put(COLUMN_MASTER_TIME, order.masterTime)
                put(COLUMN_ADDRESS, order.address)
                put(COLUMN_TEL, order.tel)
                put(COLUMN_SURPLUS, order.surplus)
                put(COLUMN_COMMENT, order.comment)
                put(COLUMN_SERVICE_FEE, order.serviceFee)
                put(COLUMN_PARTS_FEE, order.partsFee)
                put(COLUMN_SEARCH, buildSearchContent(order))
            }

            db.update(TABLE_ORDERS, values, "$COLUMN_ID=?", arrayOf(order.id.toString()))
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: updateOrder", t.message ?: "Unknown error", t)
        }
    }

    fun updateParsedOrder(order: Order) {
        try {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_STATUS, order.status.name)
                put(COLUMN_DATE, insertingDate(order.date.time))
                put(COLUMN_RECEIVED_DATE, order.receivedDate.time)
                put(COLUMN_NOTE, order.note)
                put(COLUMN_MASTER_TIME, order.masterTime)
                put(COLUMN_ADDRESS, order.address)
                put(COLUMN_TEL, order.tel)
                put(COLUMN_SURPLUS, order.surplus)
                put(COLUMN_COMMENT, order.comment)
                put(COLUMN_SERVICE_FEE, order.serviceFee)
                put(COLUMN_PARTS_FEE, order.partsFee)
                put(COLUMN_SEARCH, buildSearchContent(order))
            }

            db.update(TABLE_ORDERS, values, "$COLUMN_ID=?", arrayOf(order.id.toString()))
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: updateParsedOrder", t.message ?: "Unknown error", t)
        }
    }

    fun replaceAllOrders(orders: List<Order>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_ORDERS, null, null)
            for (order in orders) {
                addOrder(order)
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            LogWrapper.e("DatabaseHelper: replaceAllOrders", e.message ?: "Unknown error", e)
        } finally {
            db.endTransaction()
        }
    }

    fun getRangeOrders(startDate: Date, endDate: Date, status: Status? = null): List<Order> {
        val orderList = ArrayList<Order>()
        var cursor: Cursor? = null

        val selection = StringBuilder("$COLUMN_DATE >= ? AND $COLUMN_DATE <= ?")
        val selectionArgs = mutableListOf(startDate.time.toString(), endDate.time.toString())

        if (status != null) {
            selection.append(" AND $COLUMN_STATUS = ?")
            selectionArgs.add(status.name)
        }

        return try {
            val db = readableDatabase
            cursor = db.query(
                TABLE_ORDERS,
                null,
                selection.toString(),
                selectionArgs.toTypedArray(),
                null,
                null,
                "$COLUMN_DATE DESC"
            )
            if (cursor.moveToFirst()) {
                do {
                    val order = Order.Builder()
                        .id(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                        .status(Status.fromName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))))
                        .date(Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE))))
                        .receivedDate(Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECEIVED_DATE))))
                        .note(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)))
                        .masterTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_TIME)))
                        .address(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)))
                        .tel(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEL)))
                        .surplus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SURPLUS)))
                        .comment(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT)))
                        .serviceFee(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SERVICE_FEE)))
                        .partsFee(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PARTS_FEE)))
                        .build()
                    orderList.add(order)
                } while (cursor.moveToNext())
            }
            orderList
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: getRangeOrders", t.message ?: "Unknown error", t)
            emptyList()
        } finally {
            cursor?.close()
        }
    }

    fun searchOrders(query: String): List<Order> {
        val orderList = ArrayList<Order>()
        var cursor: Cursor? = null
        return try {
            val db = readableDatabase
            val selection = "$COLUMN_SEARCH LIKE ?"
            val selectionArgs = arrayOf("%${query.lowercase(Locale.getDefault())}%")
            cursor = db.query(
                TABLE_ORDERS,
                null,
                selection,
                selectionArgs,
                null,
                null,
                "$COLUMN_DATE DESC"
            )
            if (cursor.moveToFirst()) {
                do {
                    val order = Order.Builder()
                        .id(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)))
                        .status(Status.fromName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS))))
                        .date(Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE))))
                        .receivedDate(Date(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_RECEIVED_DATE))))
                        .note(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTE)))
                        .masterTime(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MASTER_TIME)))
                        .address(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)))
                        .tel(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEL)))
                        .surplus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SURPLUS)))
                        .comment(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENT)))
                        .serviceFee(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SERVICE_FEE)))
                        .partsFee(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PARTS_FEE)))
                        .build()
                    orderList.add(order)
                } while (cursor.moveToNext())
            }
            orderList
        } catch (t: Throwable) {
            LogWrapper.e("DatabaseHelper: searchOrders", t.message ?: "Unknown error", t)
            emptyList()
        } finally {
            cursor?.close()
        }
    }

    companion object {
        private const val DATABASE_NAME = "sms.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_ORDERS = "orders"
        private const val COLUMN_ID = "id"
        private const val COLUMN_STATUS = "status"
        private const val COLUMN_DATE = "date"
        private const val COLUMN_RECEIVED_DATE = "received_date"
        private const val COLUMN_NOTE = "note"
        private const val COLUMN_MASTER_TIME = "master_time"
        private const val COLUMN_ADDRESS = "address"
        private const val COLUMN_TEL = "tel"
        private const val COLUMN_SURPLUS = "surplus"
        private const val COLUMN_COMMENT = "comment"
        private const val COLUMN_SERVICE_FEE = "service_fee"
        private const val COLUMN_PARTS_FEE = "parts_fee"
        private const val COLUMN_SEARCH = "search"

        @Volatile
        private var instance: DatabaseHelper? = null

        @Synchronized
        fun getInstance(context: Context): DatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun buildSearchContent(order: Order): String {
        return listOf(
            order.id,
            order.status,
            DATE_FORMATTER.format(order.date),
            DATE_FORMATTER.format(order.receivedDate),
            order.note,
            order.masterTime,
            order.address,
            order.tel,
            order.surplus,
            order.comment,
            order.serviceFee.toString(),
            order.partsFee.toString()
        ).joinToString(" ")
    }

    private fun insertingDate(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.time.time
    }
}
