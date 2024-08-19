package com.grytsyna.fixitpro.activity.report

import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grytsyna.fixitpro.common.Constants.DATE_FORMATTER
import com.grytsyna.fixitpro.activity.export_import.ExportImportActivity
import com.grytsyna.fixitpro.R
import com.grytsyna.fixitpro.common.Constants.EMPTY
import com.grytsyna.fixitpro.common.Constants.EXTRA_IMPORT_RESULT
import com.grytsyna.fixitpro.common.Constants.REQUEST_CODE_IMPORT_DATA
import com.grytsyna.fixitpro.common.DateUtils
import com.grytsyna.fixitpro.db.DatabaseHelper
import com.grytsyna.fixitpro.enum_status.Status
import java.util.Calendar

class ReportActivity : AppCompatActivity() {

    private lateinit var etFromDate: EditText
    private lateinit var etToDate: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvSummaryServiceFee: TextView
    private lateinit var tvSummaryPartsFee: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnOpenCalculator: ImageButton
    private lateinit var btnCopyServiceFee: ImageButton
    private lateinit var btnCopyPartsFee: ImageButton
    private lateinit var btnCopyTotal: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        etFromDate = findViewById(R.id.etFromDate)
        etToDate = findViewById(R.id.etToDate)
        tvSummaryServiceFee = findViewById(R.id.tvSummaryServiceFee)
        tvSummaryPartsFee = findViewById(R.id.tvSummaryPartsFee)
        tvTotal = findViewById(R.id.tvTotal)
        btnOpenCalculator = findViewById(R.id.btnOpenCalculator)
        btnCopyServiceFee = findViewById(R.id.btnCopyServiceFee)
        btnCopyPartsFee = findViewById(R.id.btnCopyPartsFee)
        btnCopyTotal = findViewById(R.id.btnCopyTotal)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val defaultFee = "0.0"
        tvSummaryServiceFee.text = getString(R.string.label_summary_service_fee_report, defaultFee)
        tvSummaryPartsFee.text = getString(R.string.label_summary_parts_fee_report, defaultFee)
        tvTotal.text = getString(R.string.label_total_report, defaultFee)

        val btnBack: Button = findViewById(R.id.btnBack)
        val btnForward: Button = findViewById(R.id.btnForward)
        val btnShowOrders: Button = findViewById(R.id.btnShowOrders)

        btnBack.setOnClickListener {
            finish()
        }

        btnForward.setOnClickListener {
            val intent = Intent(this, ExportImportActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_IMPORT_DATA)
        }

        etFromDate.setOnClickListener {
            showDatePickerDialog(etFromDate)
        }

        etToDate.setOnClickListener {
            showDatePickerDialog(etToDate)
        }

        btnShowOrders.setOnClickListener {
            showOrders()
        }

        btnCopyServiceFee.setOnClickListener {
            copyToClipboard(tvSummaryServiceFee)
        }

        btnCopyPartsFee.setOnClickListener {
            copyToClipboard(tvSummaryPartsFee)
        }

        btnCopyTotal.setOnClickListener {
            copyToClipboard(tvTotal)
        }

        btnOpenCalculator.setOnClickListener {
            openCalculator()
        }
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

    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)
                editText.setText(DATE_FORMATTER.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showOrders() {
        if (etFromDate.text.isEmpty() || etToDate.text.isEmpty()) {
            Toast.makeText(this, getString(R.string.empty_dates_toast), Toast.LENGTH_SHORT).show()
            return
        }

        val fromDate = DateUtils.getStartDateOrToday(etFromDate)
        val toDate = DateUtils.getEndDateOrToday(etToDate)

        val orders = DatabaseHelper.getInstance(this).getRangeOrders(fromDate, toDate, Status.COMPLETED)

        val adapter = OrderAdapterReport(orders)
        recyclerView.adapter = adapter

        val totalServiceFee = orders.sumOf { it.serviceFee }
        val totalPartsFee = orders.sumOf { it.partsFee }
        val total = totalServiceFee + totalPartsFee

        tvSummaryServiceFee.text = getString(R.string.label_summary_service_fee_report, totalServiceFee.toString())
        tvSummaryPartsFee.text = getString(R.string.label_summary_parts_fee_report, totalPartsFee.toString())
        tvTotal.text = getString(R.string.label_total_report, total.toString())
    }

    private fun copyToClipboard(textView: TextView) {
        val text = textView.text.toString().replace("[^\\d.]".toRegex(), EMPTY)
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(EMPTY, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied_to_clipboard_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCalculator() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALCULATOR)
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.calculator_not_found_toast), Toast.LENGTH_SHORT).show()
        }
    }
}
