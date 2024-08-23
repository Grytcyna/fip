package com.grytsyna.fixitpro

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.grytsyna.fixitpro.activity.edit.EditOrderActivity
import com.grytsyna.fixitpro.activity.main.OnOrdersLoadedListener
import com.grytsyna.fixitpro.activity.main.PermissionManager
import com.grytsyna.fixitpro.activity.main.order.OrderAdapter
import com.grytsyna.fixitpro.activity.main.order.OrderViewModel
import com.grytsyna.fixitpro.activity.search.SearchActivity
import com.grytsyna.fixitpro.common.Constants
import com.grytsyna.fixitpro.common.Constants.DATE_FORMATTER
import com.grytsyna.fixitpro.common.Constants.EXTRA_ORDER
import com.grytsyna.fixitpro.common.DateUtils
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.db.DatabaseHelper
import com.grytsyna.fixitpro.entity.Order
import com.grytsyna.fixitpro.enum_status.RangeFilter
import com.grytsyna.fixitpro.enum_status.Status
import com.grytsyna.fixitpro.receiver.MyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.util.*

class MainActivity : AppCompatActivity(), OnOrdersLoadedListener {

    private lateinit var viewModel: OrderViewModel
    private lateinit var recyclerViewNew: RecyclerView
    private lateinit var recyclerViewInProcess: RecyclerView
    private lateinit var recyclerViewCompleted: RecyclerView
    private lateinit var recyclerViewCanceled: RecyclerView
    private lateinit var recyclerViewDeleted: RecyclerView
    private lateinit var adapterNew: OrderAdapter
    private lateinit var adapterInProcess: OrderAdapter
    private lateinit var adapterCompleted: OrderAdapter
    private lateinit var adapterCanceled: OrderAdapter
    private lateinit var adapterDeleted: OrderAdapter
    private lateinit var etFromDate: EditText
    private lateinit var etToDate: EditText
    private lateinit var tabs: TabLayout
    private lateinit var btnForward: Button
    private lateinit var spinnerFilter: Spinner
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var permissionManager: PermissionManager

    private var allOrders: MutableList<Order> = mutableListOf()
    private var tempOrders: MutableList<Order> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        (application as MyApplication).setOrdersLoadedListener(this)
        (application as MyApplication).mainActivity = this

        setupPermissions()

        initOrderViewModelAndDB()

        initUI()

        setupAdapters()

        setupRecyclerViews()

        setupTabs()

        setupFilters()

        setupDefaultFilters()

        initObservers()
    }

    override fun onDestroy() {
        super.onDestroy()
        (application as MyApplication).removeOrdersLoadedListener()
        (application as MyApplication).mainActivity = null
    }

    override fun onOrdersLoaded(orders: List<Order>) {
        allOrders.clear()
        allOrders.addAll(orders)
        updateOrderLists(allOrders)
        filterOrders()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.handlePermissionsResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_CODE_EDIT_ORDER && resultCode == RESULT_OK && data != null) {
            val updatedOrder: Order? = data?.getParcelableExtra(EXTRA_ORDER, Order::class.java)
            if (updatedOrder != null) {
                val position = allOrders.indexOfFirst { it.id == updatedOrder.id }
                if (position != -1) {
                    allOrders[position] = updatedOrder
                } else {
                    allOrders.add(updatedOrder)
                }

                if (tempOrders.isNotEmpty()) {
                    val tempPosition = tempOrders.indexOfFirst { it.id == updatedOrder.id }
                    if (tempPosition != -1) {
                        tempOrders[tempPosition] = updatedOrder
                    } else {
                        tempOrders.add(updatedOrder)
                    }
                }

                updateOrderLists(allOrders)
                filterOrders()
            }
        }
    }

    override fun onOrderAdded(order: Order) {
        val position = allOrders.indexOfFirst { it.id == order.id }
        if (position != -1) {
            allOrders[position] = order
        } else {
            allOrders.add(order)
        }

        updateOrderLists(allOrders)
        filterOrders()
    }

    private fun setupPermissions() {
        permissionManager = PermissionManager(this)
        if (permissionManager.checkAndRequestPermissions()) {
            (application as MyApplication).readSmsFromArchive()
        }
    }

    private fun initOrderViewModelAndDB() {
        viewModel = OrderViewModel.getInstance()
        databaseHelper = DatabaseHelper.getInstance(this)
        viewModel.setDatabaseHelper(databaseHelper)
    }

    private fun initUI() {
        recyclerViewNew = findViewById(R.id.recyclerViewNew)
        recyclerViewInProcess = findViewById(R.id.recyclerViewInProcess)
        recyclerViewCompleted = findViewById(R.id.recyclerViewCompleted)
        recyclerViewCanceled = findViewById(R.id.recyclerViewCanceled)
        recyclerViewDeleted = findViewById(R.id.recyclerViewDeleted)
        etFromDate = findViewById(R.id.etFromDate)
        etToDate = findViewById(R.id.etToDate)
        tabs = findViewById(R.id.tabs)
        btnForward = findViewById(R.id.btnForward)
        spinnerFilter = findViewById(R.id.spinnerFilter)

        etFromDate.setOnClickListener {
            if (RangeFilter.fromDescription(spinnerFilter.selectedItem.toString()) == RangeFilter.MANUALLY) {
                showDatePickerDialog(etFromDate) { selectedDate ->
                    if (!etToDate.text.isNullOrEmpty()) {
                        val fromDate = DateUtils.getStartDateOrToday(etFromDate)
                        val toDate = DateUtils.getEndDateOrToday(etToDate)
                        fetchOrdersForCustomDateRange(fromDate, toDate)
                    }
                }
            } else {
                showDatePickerDialog(etFromDate)
            }
        }

        etToDate.setOnClickListener {
            if (RangeFilter.fromDescription(spinnerFilter.selectedItem.toString()) == RangeFilter.MANUALLY) {
                showDatePickerDialog(etToDate) { selectedDate ->
                    if (!etFromDate.text.isNullOrEmpty()) {
                        val fromDate = DateUtils.getStartDateOrToday(etFromDate)
                        val toDate = DateUtils.getEndDateOrToday(etToDate)
                        fetchOrdersForCustomDateRange(fromDate, toDate)
                    }
                }
            } else {
                showDatePickerDialog(etToDate)
            }
        }

        btnForward.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivityForResult(intent, Constants.REQUEST_CODE_IMPORT_DATA)
        }
    }

    private fun setupAdapters() {
        adapterNew = createAdapter()
        adapterInProcess = createAdapter()
        adapterCompleted = createAdapter()
        adapterCanceled = createAdapter()
        adapterDeleted = createAdapter()
    }

    private fun setupRecyclerViews() {
        setupRecyclerView(recyclerViewNew, adapterNew)
        setupRecyclerView(recyclerViewInProcess, adapterInProcess)
        setupRecyclerView(recyclerViewCompleted, adapterCompleted)
        setupRecyclerView(recyclerViewCanceled, adapterCanceled)
        setupRecyclerView(recyclerViewDeleted, adapterDeleted)

        setupItemTouchHelper(recyclerViewNew, adapterNew)
        setupItemTouchHelper(recyclerViewInProcess, adapterInProcess)
        setupItemTouchHelper(recyclerViewCompleted, adapterCompleted)
        setupItemTouchHelper(recyclerViewCanceled, adapterCanceled)
        setupItemTouchHelper(recyclerViewDeleted, adapterDeleted)
    }

    private fun setupTabs() {
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewModel.setCurrentTabPosition(tab.position)
                updateRecyclerViewVisibility(tab.position)
                filterOrders()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        setupTabBackgrounds()
    }

    private fun setupFilters() {
        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (RangeFilter.fromIndex(position)) {
                    RangeFilter.TODAY -> {
                        setDatesToToday()
                        disableDateInputs()
                        restoreOrdersFromTemp()
                    }

                    RangeFilter.TOMORROW -> {
                        setDatesToTomorrow()
                        disableDateInputs()
                        restoreOrdersFromTemp()
                    }

                    RangeFilter.WEEK -> {
                        setDatesToThisWeek()
                        disableDateInputs()
                        restoreOrdersFromTemp()
                    }

                    RangeFilter.MONTH -> {
                        setDatesToThisMonth()
                        disableDateInputs()
                        restoreOrdersFromTemp()
                    }

                    RangeFilter.MANUALLY -> {
                        saveOrdersToTemp()
                        enableDateInputs()
                        clearDateFields()
                    }
                }

                filterOrders()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun restoreOrdersFromTemp() {
        if (tempOrders.isNotEmpty()) {
            allOrders.clear()
            allOrders.addAll(tempOrders)
            tempOrders.clear()
            updateOrderLists(allOrders)
        }
    }

    private fun saveOrdersToTemp() {
        if (tempOrders.isEmpty()) {
            tempOrders.addAll(allOrders)
            allOrders.clear()
        }
    }

    private fun clearDateFields() {
        etFromDate.text.clear()
        etToDate.text.clear()
    }

    private fun fetchOrdersForCustomDateRange(fromDate: Date, toDate: Date) {
        CoroutineScope(Dispatchers.IO).launch {
            val orders = databaseHelper.getRangeOrders(fromDate, toDate)
            withContext(Dispatchers.Main) {
                tempOrders.clear()
                tempOrders.addAll(allOrders)

                allOrders.clear()
                allOrders.addAll(orders)

                updateOrderLists(allOrders)
            }
        }
    }

    private fun setupDefaultFilters() {
        spinnerFilter.setSelection(1)
        setDatesToTomorrow()
    }

    private fun initObservers() {
        viewModel.getOrders().observe(this) { orders ->
            updateOrderLists(orders)
            filterOrders()
        }

        viewModel.getCurrentTabPosition().observe(this) { position ->
            tabs.getTabAt(position)?.select()
            updateRecyclerViewVisibility(position)
        }
    }

    private fun createAdapter(): OrderAdapter {
        return OrderAdapter(object : OrderAdapter.OnItemClickListener {
            override fun onItemClick(order: Order) {
                editOrder(order)
            }
        })
    }

    private fun setupRecyclerView(recyclerView: RecyclerView, adapter: OrderAdapter) {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun setupItemTouchHelper(recyclerView: RecyclerView, adapter: OrderAdapter) {
        val itemTouchHelperCallback = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val order = adapter.currentList[position]

                val initialStatus = order.status
                val newStatus = when (direction) {
                    ItemTouchHelper.RIGHT -> getUpdatedStatus(order, true)
                    ItemTouchHelper.LEFT -> getUpdatedStatus(order, false)
                    else -> initialStatus
                }

                if (newStatus == initialStatus) {
                    adapter.notifyItemChanged(position)
                } else {
                    updateOrderStatus(order, newStatus)
                }
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun getUpdatedStatus(order: Order, moveForward: Boolean): Status {
        val status = when (order.status) {
            Status.NEW -> if (moveForward) Status.IN_PROCESS else Status.NEW
            Status.IN_PROCESS -> if (moveForward) Status.COMPLETED else Status.NEW
            Status.COMPLETED -> if (moveForward) Status.CANCELED else Status.IN_PROCESS
            Status.CANCELED -> if (moveForward) Status.DELETED else Status.COMPLETED
            Status.DELETED -> if (moveForward) Status.DELETED else Status.CANCELED
        }

        if (order.status == Status.IN_PROCESS && moveForward && order.comment.isEmpty()) {
            showAddCommentDialog(order)
        }

        return status
    }

    private fun updateOrderStatus(order: Order, newStatus: Status) {
        val updatedOrder = order.copy(status = newStatus)
        viewModel.updateOrder(updatedOrder) { result ->
            runOnUiThread {
                if (result != null) {
                    val position = allOrders.indexOfFirst { it.id == result.id }
                    if (position != -1) {
                        allOrders[position] = result
                    } else {
                        allOrders.add(result)
                    }
                    updateOrderLists(allOrders)
                    filterOrders()
                } else {
                    Toast.makeText(this, getString(R.string.order_saving_error_toast), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showAddCommentDialog(order: Order) {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_comment, null)
            val editComment = dialogView.findViewById<EditText>(R.id.editComment)
            val editServiceFee = dialogView.findViewById<EditText>(R.id.editServiceFee)
            val editPartsFee = dialogView.findViewById<EditText>(R.id.editPartsFee)
            val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)

            editComment.setText(order.comment)
            editServiceFee.setText(order.serviceFee.toString())
            editPartsFee.setText(order.partsFee.toString())

            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            buttonSave.setOnClickListener {
                val comment = editComment.text.toString().trim()
                val serviceFee = editServiceFee.text.toString().toDoubleOrNull() ?: 0.0
                val partsFee = editPartsFee.text.toString().toDoubleOrNull() ?: 0.0

                if (comment.isNotEmpty()) {
                    val updatedOrder = order.copy(
                        comment = comment,
                        serviceFee = serviceFee,
                        partsFee = partsFee
                    )

                    viewModel.updateOrder(updatedOrder) { result ->
                        runOnUiThread {
                            if (result != null) {
                                val finalUpdatedOrder = result.copy(status = Status.COMPLETED)
                                viewModel.updateOrder(finalUpdatedOrder) { finalResult ->
                                    runOnUiThread {
                                        if (finalResult != null) {
                                            val position = allOrders.indexOfFirst { it.id == order.id }
                                            if (position != -1) {
                                                allOrders[position] = finalUpdatedOrder
                                                updateOrderLists(allOrders)
                                                filterOrders()
                                            }
                                            dialog.dismiss()
                                        } else {
                                            Toast.makeText(this, getString(R.string.order_saving_error_toast), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(this, getString(R.string.order_saving_error_toast), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    editComment.error = getString(R.string.comment_required_text)
                }
            }

            dialog.show()
        } catch (t: Throwable) {
            LogWrapper.e("MainActivity: showAddCommentDialog", "Error showing add comment dialog: ${t.message}", t)
        }
    }

    private fun updateOrderLists(orders: List<Order>?) {
        allOrders = orders?.toMutableList() ?: mutableListOf()

        sortOrdersByDateAndTime()

        adapterNew.submitList(allOrders.filter { it.status == Status.NEW })
        adapterInProcess.submitList(allOrders.filter { it.status == Status.IN_PROCESS })
        adapterCompleted.submitList(allOrders.filter { it.status == Status.COMPLETED })
        adapterCanceled.submitList(allOrders.filter { it.status == Status.CANCELED })
        adapterDeleted.submitList(allOrders.filter { it.status == Status.DELETED })
    }

    private fun sortOrdersByDateAndTime() {
        allOrders = allOrders.sortedWith(compareByDescending<Order> {
            it.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }.thenBy {
            parseMasterTime(it.masterTime)
        }).toMutableList()
    }

    private fun parseMasterTime(masterTime: String): Int {
        val startTime = masterTime.split("-").firstOrNull()?.trim()
        return startTime?.let { Constants.TIME_ORDER.indexOf(it) } ?: Int.MAX_VALUE
    }

    private fun filterOrders() {
        val status = tabs.selectedTabPosition
        val selectedStatus = Status.entries[status]
        val fromDate = DateUtils.getStartDateOrToday(etFromDate)
        val toDate = DateUtils.getEndDateOrToday(etToDate)

        val filteredOrders = allOrders.filter { order ->
            order.status == selectedStatus && order.date >= fromDate && order.date <= toDate
        }

        when (status) {
            0 -> adapterNew.submitList(filteredOrders)
            1 -> adapterInProcess.submitList(filteredOrders)
            2 -> adapterCompleted.submitList(filteredOrders)
            3 -> adapterCanceled.submitList(filteredOrders)
            4 -> adapterDeleted.submitList(filteredOrders)
        }
    }

    private fun showDatePickerDialog(editText: EditText, onDateSelected: ((Date) -> Unit)? = null) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)
                editText.setText(DATE_FORMATTER.format(selectedDate.time))

                onDateSelected?.invoke(selectedDate.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun enableDateInputs() {
        etFromDate.isEnabled = true
        etToDate.isEnabled = true
    }

    private fun disableDateInputs() {
        etFromDate.isEnabled = false
        etToDate.isEnabled = false
    }

    private fun setDatesToToday() {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        etFromDate.setText(DATE_FORMATTER.format(today))
        etToDate.setText(DATE_FORMATTER.format(today))
    }

    private fun setDatesToTomorrow() {
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = calendar.time
        etFromDate.setText(DATE_FORMATTER.format(today))
        etToDate.setText(DATE_FORMATTER.format(tomorrow))
    }

    private fun setDatesToThisWeek() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val startOfWeek = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfWeek = calendar.time
        etFromDate.setText(DATE_FORMATTER.format(startOfWeek))
        etToDate.setText(DATE_FORMATTER.format(endOfWeek))
    }

    private fun setDatesToThisMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.time
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val endOfMonth = calendar.time
        etFromDate.setText(DATE_FORMATTER.format(startOfMonth))
        etToDate.setText(DATE_FORMATTER.format(endOfMonth))
    }

    private fun updateRecyclerViewVisibility(position: Int) {
        recyclerViewNew.visibility = if (position == 0) View.VISIBLE else View.GONE
        recyclerViewInProcess.visibility = if (position == 1) View.VISIBLE else View.GONE
        recyclerViewCompleted.visibility = if (position == 2) View.VISIBLE else View.GONE
        recyclerViewCanceled.visibility = if (position == 3) View.VISIBLE else View.GONE
        recyclerViewDeleted.visibility = if (position == 4) View.VISIBLE else View.GONE
    }

    private fun setupTabBackgrounds() {
        tabs.getTabAt(0)?.view?.setBackgroundColor(
            ContextCompat.getColor(this, R.color.color_default_new)
        )
        tabs.getTabAt(1)?.view?.setBackgroundColor(
            ContextCompat.getColor(this, R.color.color_default_in_process)
        )
        tabs.getTabAt(2)?.view?.setBackgroundColor(
            ContextCompat.getColor(this, R.color.color_default_completed)
        )
        tabs.getTabAt(3)?.view?.setBackgroundColor(
            ContextCompat.getColor(this, R.color.color_default_canceled)
        )
        tabs.getTabAt(4)?.view?.setBackgroundColor(
            ContextCompat.getColor(this, R.color.color_default_deleted)
        )
    }

    private fun editOrder(order: Order) {
        val intent = Intent(this, EditOrderActivity::class.java)
        intent.putExtra(Constants.EXTRA_ORDER, order)
        startActivityForResult(intent, Constants.REQUEST_CODE_EDIT_ORDER)
    }
}
