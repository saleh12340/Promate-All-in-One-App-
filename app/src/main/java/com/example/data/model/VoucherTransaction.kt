package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class VoucherType(val labelAr: String) {
    RECEIPT("سند قبض"),
    PAYMENT("سند صرف"),
    INVOICE_CREDIT("فاتورة آجلة")
}

@Entity(
    tableName = "vouchers",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class VoucherTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val voucherType: VoucherType,
    val amount: Double,
    val debit: Double = 0.0,
    val credit: Double = 0.0,
    val runningBalance: Double = 0.0,
    val statement: String,
    val invoiceId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
