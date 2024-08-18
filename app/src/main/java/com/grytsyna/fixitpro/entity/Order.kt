package com.grytsyna.fixitpro.entity

import android.os.Parcel
import android.os.Parcelable
import com.grytsyna.fixitpro.enum_status.Status
import java.util.*

data class Order(
    val id: Int,
    val status: Status,
    val date: Date,
    val receivedDate: Date,
    val note: String,
    val masterTime: String,
    val address: String,
    val tel: String,
    val surplus: String,
    val comment: String,
    val serviceFee: Double,
    val partsFee: Double
) : Parcelable {

    class Builder {
        private var id: Int = 0
        private var status: Status? = null
        private var date: Date? = null
        private var receivedDate: Date? = null
        private var note: String? = null
        private var masterTime: String? = null
        private var address: String? = null
        private var tel: String? = null
        private var surplus: String? = null
        private var comment: String? = null
        private var serviceFee: Double = 0.0
        private var partsFee: Double = 0.0

        fun id(id: Int) = apply { this.id = id }
        fun status(status: Status) = apply { this.status = status }
        fun date(date: Date) = apply { this.date = date }
        fun receivedDate(receivedDate: Date) = apply { this.receivedDate = receivedDate }
        fun note(note: String) = apply { this.note = note }
        fun masterTime(masterTime: String) = apply { this.masterTime = masterTime }
        fun address(address: String) = apply { this.address = address }
        fun tel(tel: String) = apply { this.tel = tel }
        fun surplus(surplus: String) = apply { this.surplus = surplus }
        fun comment(comment: String) = apply { this.comment = comment }
        fun serviceFee(serviceFee: Double) = apply { this.serviceFee = serviceFee }
        fun partsFee(partsFee: Double) = apply { this.partsFee = partsFee }

        fun build() = Order(
            id,
            status ?: throw IllegalArgumentException("Status must not be null"),
            date ?: throw IllegalArgumentException("Date must not be null"),
            receivedDate ?: throw IllegalArgumentException("Received date must not be null"),
            note ?: throw IllegalArgumentException("Note must not be null"),
            masterTime ?: throw IllegalArgumentException("Master time must not be null"),
            address ?: throw IllegalArgumentException("Address must not be null"),
            tel ?: throw IllegalArgumentException("Tel must not be null"),
            surplus ?: throw IllegalArgumentException("Surplus must not be null"),
            comment ?: throw IllegalArgumentException("Comment must not be null"),
            serviceFee,
            partsFee
        )
    }

    fun toBuilder() = Builder()
        .id(this.id)
        .status(this.status)
        .date(this.date)
        .receivedDate(this.receivedDate)
        .note(this.note)
        .masterTime(this.masterTime)
        .address(this.address)
        .tel(this.tel)
        .surplus(this.surplus)
        .comment(this.comment)
        .serviceFee(this.serviceFee)
        .partsFee(this.partsFee)

    override fun toString(): String {
        return "Order(id='$id', status=$status, date=$date, receivedDate=$receivedDate, note='$note', masterTime='$masterTime', address='$address', tel='$tel', surplus='$surplus', comment='$comment', serviceFee='$serviceFee', partsFee='$partsFee')"
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(status.name)
        parcel.writeLong(date.time)
        parcel.writeLong(receivedDate.time)
        parcel.writeString(note)
        parcel.writeString(masterTime)
        parcel.writeString(address)
        parcel.writeString(tel)
        parcel.writeString(surplus)
        parcel.writeString(comment)
        parcel.writeDouble(serviceFee)
        parcel.writeDouble(partsFee)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Order> {
        override fun createFromParcel(parcel: Parcel): Order {
            return Order(
                parcel.readInt(),
                Status.valueOf(parcel.readString() ?: Status.DELETED.name),
                Date(parcel.readLong()),
                Date(parcel.readLong()),
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readString() ?: "",
                parcel.readDouble(),
                parcel.readDouble()
            )
        }

        override fun newArray(size: Int): Array<Order?> = arrayOfNulls(size)
    }
}
