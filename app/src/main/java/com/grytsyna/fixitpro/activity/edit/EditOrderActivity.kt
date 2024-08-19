package com.grytsyna.fixitpro.activity.edit

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.grytsyna.fixitpro.common.Constants.DATE_FORMATTER
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.entity.Order
import com.grytsyna.fixitpro.R
import com.grytsyna.fixitpro.activity.main.order.OrderViewModel
import com.grytsyna.fixitpro.common.Constants.EMPTY
import com.grytsyna.fixitpro.common.Constants.EXTRA_GEO
import com.grytsyna.fixitpro.common.Constants.EXTRA_ORDER
import com.grytsyna.fixitpro.common.Constants.EXTRA_TEL
import com.grytsyna.fixitpro.databinding.ActivityEditOrderBinding
import com.grytsyna.fixitpro.db.DatabaseHelper
import com.grytsyna.fixitpro.enum_status.Status
import java.util.Calendar
import java.util.Date

class EditOrderActivity : AppCompatActivity() {

    private lateinit var viewModel: OrderViewModel
    private lateinit var order: Order
    private var isEditing = false
    private lateinit var binding: ActivityEditOrderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = OrderViewModel.getInstance().apply {
            setDatabaseHelper(DatabaseHelper.getInstance(this@EditOrderActivity))
        }

        order = intent.getParcelableExtra(EXTRA_ORDER, Order::class.java)
            ?: throw IllegalArgumentException("Order is missing")

        val statuses = resources.getStringArray(R.array.order_status_array)
        val adapter = StatusSpinnerAdapter(this, statuses)
        binding.spOrderStatus.adapter = adapter

        binding.spOrderStatus.setSelection(adapter.getPosition(order.status.name))
        binding.etOrderId.setText(order.id.toString())
        binding.etOrderDate.setText(DATE_FORMATTER.format(order.date))
        binding.etNote.setText(order.note)
        binding.etMasterTime.setText(order.masterTime)
        binding.etAddress.setText(order.address)
        binding.etTel.setText(order.tel)
        binding.etSurplus.setText(order.surplus)
        binding.etComment.setText(order.comment)
        binding.etServiceFee.setText(order.serviceFee.toString())
        binding.etPartsFee.setText(order.partsFee.toString())

        binding.etOrderDate.setOnClickListener { showDatePickerDialog() }

        binding.btnOpenMap.setOnClickListener {
            val address = binding.etAddress.text.toString()
            openGoogleMaps(address)
        }

        binding.btnCall.setOnClickListener {
            val tel = binding.etTel.text.toString()
            if (tel.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("${EXTRA_TEL}${tel}")
                }
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this, getString(R.string.phone_handle_request_toast), Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, getString(R.string.phone_number_empty_toast), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEdit.setOnClickListener {
            if (isEditing) {
                order = order.toBuilder()
                    .status(Status.fromName(binding.spOrderStatus.selectedItem.toString().trim()))
                    .date(parseDate(binding.etOrderDate.text.toString().trim()))
                    .note(binding.etNote.text.toString().trim())
                    .masterTime(binding.etMasterTime.text.toString().trim())
                    .address(binding.etAddress.text.toString().trim())
                    .tel(binding.etTel.text.toString().trim())
                    .surplus(binding.etSurplus.text.toString().trim())
                    .comment(binding.etComment.text.toString().trim())
                    .serviceFee(binding.etServiceFee.text.toString().trim().toDoubleOrNull() ?: 0.0)
                    .partsFee(binding.etPartsFee.text.toString().trim().toDoubleOrNull() ?: 0.0)
                    .build()

                viewModel.updateOrder(order)
                onBackPressedDispatcher.onBackPressed()
            } else {
                enableEditing(true)
            }
        }

        binding.textInputLayoutOrderId.setEndIconOnClickListener {
            copyToClipboard(binding.etOrderId)
        }

        binding.textInputLayoutAddress.setEndIconOnClickListener {
            copyToClipboard(binding.etAddress)
        }

        binding.textInputLayoutTel.setEndIconOnClickListener {
            copyToClipboard(binding.etTel)
        }

        binding.btnCancel.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        enableEditing(false)
    }

    private fun enableEditing(enable: Boolean) {
        isEditing = enable
        binding.spOrderStatus.isEnabled = enable
        binding.etOrderDate.isEnabled = enable
        binding.etNote.isEnabled = enable
        binding.etMasterTime.isEnabled = enable
        binding.etAddress.isEnabled = enable
        binding.etTel.isEnabled = enable
        binding.etSurplus.isEnabled = enable
        binding.etComment.isEnabled = enable
        binding.etServiceFee.isEnabled = enable
        binding.etPartsFee.isEnabled = enable

        binding.etAddress.isFocusable = enable
        binding.etAddress.isFocusableInTouchMode = enable
        binding.etAddress.isClickable = !enable

        binding.btnEdit.text = if (enable) getString(R.string.save_button_text) else getString(R.string.edit_button_text)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, year1, monthOfYear, dayOfMonth ->
            val formattedDate = getString(R.string.date_format, monthOfYear + 1, dayOfMonth, year1)
            binding.etOrderDate.setText(formattedDate)
        }, year, month, day)
        datePickerDialog.show()
    }

    private fun openGoogleMaps(address: String?) {
        if (address.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.address_empty_toast), Toast.LENGTH_SHORT).show()
            return
        }

        val gmmIntentUri = Uri.parse("${EXTRA_GEO}${Uri.encode(address)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).setPackage(getString(R.string.google_maps))

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, getString(R.string.map_handle_request_toast), Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseDate(dateString: String): Date {
        return try {
            DATE_FORMATTER.parse(dateString) ?: throw IllegalArgumentException("Invalid date format")
        } catch (e: Exception) {
            LogWrapper.e("DatabaseHelper", "Error parsing date: ${e.message}", e)
            Date()
        }
    }

    private fun copyToClipboard(editText: EditText) {
        val text = editText.text.toString()
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(EMPTY, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied_to_clipboard_toast), Toast.LENGTH_SHORT).show()
        }
    }
}
