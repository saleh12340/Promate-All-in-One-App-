package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.VoucherTransaction

@Database(
    entities = [
        Customer::class,
        Invoice::class,
        InvoiceItem::class,
        VoucherTransaction::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun voucherDao(): VoucherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun closeDatabase() { synchronized(this) { INSTANCE?.close(); INSTANCE = null } }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_invoicing_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
