package com.grytsyna.fixitpro.activity.report

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.grytsyna.fixitpro.common.Constants.DATE_FORMATTER
import com.grytsyna.fixitpro.entity.Order
import com.grytsyna.fixitpro.R

class OrderAdapterReport(private val orders: List<Order>) :
    RecyclerView.Adapter<OrderAdapterReport.OrderViewHolder>() {

    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvOrderId: TextView = itemView.findViewById(R.id.tvOrderId)
        val tvOrderDate: TextView = itemView.findViewById(R.id.tvOrderDate)
        val tvNoteAndSurplus: TextView = itemView.findViewById(R.id.tvNoteAndSurplus)
        val tvComment: TextView = itemView.findViewById(R.id.tvComment)
        val tvServiceFee: TextView = itemView.findViewById(R.id.tvServiceFee)
        val tvPartsFee: TextView = itemView.findViewById(R.id.tvPartsFee)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        return OrderViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_order_report, parent, false)
        )
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        val context = holder.itemView.context

        holder.tvOrderId.text = order.id.toString()
        holder.tvOrderDate.text = DATE_FORMATTER.format(order.date)
        holder.tvNoteAndSurplus.text = context.getString(R.string.note_surplus_report, order.note, order.surplus)
        holder.tvComment.text = context.getString(R.string.comment_report, order.comment)
        holder.tvServiceFee.text = context.getString(
            R.string.service_fee_report,
            order.serviceFee.toString()
        )
        holder.tvPartsFee.text = context.getString(
            R.string.parts_fee_report,
            order.partsFee.toString()
        )
    }

    override fun getItemCount(): Int {
        return orders.size
    }
}
