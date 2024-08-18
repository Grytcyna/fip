package com.grytsyna.fixitpro.activity.main.order

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.grytsyna.fixitpro.R
import com.grytsyna.fixitpro.entity.Order
import java.text.SimpleDateFormat
import java.util.Locale

class OrderAdapter(
    private val listener: OnItemClickListener
) : ListAdapter<Order, OrderAdapter.OrderViewHolder>(DIFF_CALLBACK) {

    interface OnItemClickListener {
        fun onItemClick(order: Order)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position), listener)
    }

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMasterTime: TextView = itemView.findViewById(R.id.tvMasterTime)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvNoteAndSurplus: TextView = itemView.findViewById(R.id.tvNoteAndSurplus)

        fun bind(order: Order, listener: OnItemClickListener) {
            val formattedDate = SimpleDateFormat(itemView.context.getString(R.string.order_date_format), Locale.US)
                .format(order.date)

            tvMasterTime.text = itemView.context.getString(
                R.string.order_text_format,
                formattedDate,
                order.masterTime
            )

            tvAddress.text = order.address

            tvNoteAndSurplus.text = itemView.context.getString(
                R.string.order_text_format,
                order.note,
                order.surplus
            )

            itemView.setOnClickListener { listener.onItemClick(order) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Order>() {
            override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean {
                return oldItem == newItem
            }
        }
    }
}
