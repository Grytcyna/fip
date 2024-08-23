package com.grytsyna.fixitpro.activity.export_import

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.grytsyna.fixitpro.entity.Order
import com.grytsyna.fixitpro.R
import com.grytsyna.fixitpro.common.Constants.DATE_FORMATTER
import com.grytsyna.fixitpro.common.Constants.REQUEST_CODE_CREATE_FILE
import com.grytsyna.fixitpro.common.Constants.REQUEST_CODE_OPEN_FILE
import com.grytsyna.fixitpro.db.DatabaseHelper
import com.grytsyna.fixitpro.enum_status.Status
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Date

class ExportImportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_import)

        val btnBack: Button = findViewById(R.id.btnBack)
        val btnExport: Button = findViewById(R.id.btnExport)
        val btnImport: Button = findViewById(R.id.btnImport)

        btnBack.setOnClickListener {
            finish()
        }

        btnExport.setOnClickListener {
            createFile()
        }

        btnImport.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.attention_message_title))
                .setMessage(getString(R.string.attention_dialog_message_text))
                .setPositiveButton(getString(R.string.yes_button_text)) { _, _ ->
                    openFilePicker()
                }
                .setNegativeButton(getString(R.string.no_button_text), null)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_CREATE_FILE -> if (resultCode == RESULT_OK) {
                data?.data?.also { uri ->
                    exportDataToCsv(uri)
                }
            }

            REQUEST_CODE_OPEN_FILE -> if (resultCode == RESULT_OK) {
                data?.data?.also { uri ->
                    importDataFromCsv(uri)
                }
            }
        }
    }

    private fun createFile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = getString(R.string.export_file_type)
            putExtra(Intent.EXTRA_TITLE, getString(R.string.export_file_name))
        }
        startActivityForResult(intent, REQUEST_CODE_CREATE_FILE)
    }

    private fun exportDataToCsv(uri: Uri) {
        val databaseHelper = DatabaseHelper.getInstance(this)
        val orders = databaseHelper.getAllOrders()

        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                    writer.write(getString(R.string.export_fields_names))
                    for (order in orders) {
                        writer.write(
                            "${order.id},${order.status},${order.date.time},${DATE_FORMATTER.format(order.date)},${order.receivedDate.time}," +
                                    "${order.note},${order.masterTime},${order.address},${order.tel}," +
                                    "${order.surplus},${order.comment},${order.serviceFee},${order.partsFee}\n"
                        )
                    }
                    writer.flush()
                }
            }
            Toast.makeText(this, getString(R.string.export_successfully_toast), Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, getString(R.string.export_error_toast, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = getString(R.string.import_file_type)
        }
        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE)
    }

    private fun importDataFromCsv(uri: Uri) {
        val databaseHelper = DatabaseHelper.getInstance(this)
        val orders = mutableListOf<Order>()

        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",")
                        if (tokens.size >= 13) {
                            val order = Order.Builder()
                                .id(tokens[0].toInt())
                                .status(Status.fromName(tokens[1]))
                                .date(Date(tokens[2].toLong()))
                                //tokens[3] skipped, contains date human format
                                .receivedDate(Date(tokens[4].toLong()))
                                .note(tokens[5])
                                .masterTime(tokens[6])
                                .address(tokens[7])
                                .tel(tokens[8])
                                .surplus(tokens[9])
                                .comment(tokens[10])
                                .serviceFee(tokens[11].toDoubleOrNull() ?: 0.0)
                                .partsFee(tokens[12].toDoubleOrNull() ?: 0.0)
                                .build()
                            orders.add(order)
                        }
                    }
                }
            }
            databaseHelper.replaceAllOrders(orders)

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.import_success_title))
                .setMessage(getString(R.string.import_success_message))
                .setPositiveButton(getString(R.string.restart_button_text)) { _, _ ->
                    restartApp()
                }
                .setCancelable(false)
                .show()

        } catch (e: IOException) {
            Toast.makeText(this, getString(R.string.import_error_toast, e.message), Toast.LENGTH_LONG).show()
        }
    }

    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        finish()
        Runtime.getRuntime().exit(0)
    }

}
