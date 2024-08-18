package com.grytsyna.fixitpro.activity.search

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grytsyna.fixitpro.R
import com.grytsyna.fixitpro.activity.edit.EditOrderActivity
import com.grytsyna.fixitpro.activity.main.order.OrderAdapter
import com.grytsyna.fixitpro.activity.report.ReportActivity
import com.grytsyna.fixitpro.common.Constants
import com.grytsyna.fixitpro.common.Constants.EXTRA_IMPORT_RESULT
import com.grytsyna.fixitpro.common.Constants.REQUEST_CODE_IMPORT_DATA
import com.grytsyna.fixitpro.db.DatabaseHelper
import com.grytsyna.fixitpro.entity.Order

class SearchActivity : AppCompatActivity(), OrderAdapter.OnItemClickListener {

    private lateinit var searchField: EditText
    private lateinit var btnSearch: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: OrderAdapter
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        searchField = findViewById(R.id.searchField)
        btnSearch = findViewById(R.id.btnSearch)
        recyclerView = findViewById(R.id.recyclerView)
        val btnBack: Button = findViewById(R.id.btnBack)
        val btnForward: Button = findViewById(R.id.btnForward)

        dbHelper = DatabaseHelper.getInstance(this)
        adapter = OrderAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnSearch.setOnClickListener {
            val query = searchField.text.toString().trim()
            if (query.isNotEmpty()) {
                searchOrders(query)
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnForward.setOnClickListener {
            val intent = Intent(this, ReportActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_IMPORT_DATA)
        }
    }

    private fun searchOrders(query: String) {
        val orders = dbHelper.searchOrders(query)
        adapter.submitList(orders)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMPORT_DATA && resultCode == RESULT_OK) {
            val importResult = data?.getBooleanExtra(EXTRA_IMPORT_RESULT, false) ?: false
            if (importResult) {
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_IMPORT_RESULT, true)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onItemClick(order: Order) {
        val intent = Intent(this, EditOrderActivity::class.java)
        intent.putExtra(Constants.EXTRA_ORDER, order)
        startActivity(intent)
    }
}
