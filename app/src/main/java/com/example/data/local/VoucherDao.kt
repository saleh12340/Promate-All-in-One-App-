package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.VoucherTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VoucherDao {
    @Query("SELECT * FROM vouchers WHERE customerId = :customerId ORDER BY createdAt ASC")
    fun getVouchersForCustomer(customerId: Long): Flow<List<VoucherTransaction>>

    @Query("SELECT * FROM vouchers WHERE customerId = :customerId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestVoucherForCustomer(customerId: Long): VoucherTransaction?

    @Query("SELECT * FROM vouchers ORDER BY createdAt DESC")
    fun getAllVouchers(): Flow<List<VoucherTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVoucher(voucher: VoucherTransaction): Long
}
