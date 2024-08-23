package com.grytsyna.fixitpro.activity.main.order

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.db.DatabaseHelper
import com.grytsyna.fixitpro.entity.Order
import java.util.concurrent.Executors

class OrderViewModel : ViewModel() {

    private val orders: MutableLiveData<List<Order>> = MutableLiveData()
    private val currentTabPosition: MutableLiveData<Int> = MutableLiveData()
    private var databaseHelper: DatabaseHelper? = null
    private val executorService = Executors.newSingleThreadExecutor()

    init {
        orders.postValue(ArrayList())
        currentTabPosition.postValue(0)
    }

    fun setDatabaseHelper(databaseHelper: DatabaseHelper) {
        this.databaseHelper = databaseHelper
    }

    fun getOrders(): LiveData<List<Order>> = orders

    fun getCurrentTabPosition(): LiveData<Int> = currentTabPosition

    fun setCurrentTabPosition(position: Int) {
        currentTabPosition.postValue(position)
    }

    fun updateOrder(order: Order, onOrderUpdated: (Order?) -> Unit) {
        if (databaseHelper == null) {
            throw IllegalStateException("DatabaseHelper is not initialized")
        }
        executorService.execute {
            try {
                databaseHelper!!.updateOrder(order)
                onOrderUpdated(order)
            } catch (t: Throwable) {
                onOrderUpdated(null)
                LogWrapper.e("OrderViewModel: updateOrder", t.message ?: "Unknown error", t)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: OrderViewModel? = null

        fun getInstance(): OrderViewModel {
            return instance ?: synchronized(this) {
                instance ?: OrderViewModel().also { instance = it }
            }
        }
    }
}
