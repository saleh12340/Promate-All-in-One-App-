package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PaymentType(val labelAr: String) {
    CASH("نقدي"),
    CREDIT("آجل")
}

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String = "",
    val paymentType: PaymentType = PaymentType.CASH,
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
