package com.grytsyna.fixitpro.activity.edit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.grytsyna.fixitpro.common.LogWrapper
import com.grytsyna.fixitpro.R
import com.grytsyna.fixitpro.databinding.SpinnerItemBinding
import com.grytsyna.fixitpro.enum_status.Status

class StatusSpinnerAdapter(
    context: Context,
    private val statuses: Array<String>
) : ArrayAdapter<String>(context, R.layout.spinner_item, statuses) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup): View {
        return try {
            val binding: SpinnerItemBinding
            var view: View? = convertView

            if (view == null) {
                binding = SpinnerItemBinding.inflate(LayoutInflater.from(context), parent, false)
                view = binding.root
                view.tag = binding
            } else {
                binding = view.tag as SpinnerItemBinding
            }

            val status = statuses[position]
            binding.textView.text = status
            val backgroundColor = getBackgroundColorForStatus(status)
            binding.textView.setBackgroundColor(ContextCompat.getColor(context, backgroundColor))

            view
        } catch (t: Throwable) {
            LogWrapper.e("StatusSpinnerAdapter: createViewFromResource", t.message ?: "Unknown error", t)
            convertView ?: View(context)
        }
    }

    private fun getBackgroundColorForStatus(status: String): Int {
        return when (Status.fromName(status)) {
            Status.NEW -> R.color.color_default_new
            Status.IN_PROCESS -> R.color.color_default_in_process
            Status.COMPLETED -> R.color.color_default_completed
            Status.CANCELED -> R.color.color_default_canceled
            Status.DELETED -> R.color.color_default_deleted
        }
    }
}
