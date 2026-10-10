package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.PaymentType
import com.example.data.model.VoucherTransaction
import com.example.data.model.VoucherType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AccountingRepository(private val database: AppDatabase) {
    private val customerDao = database.customerDao()
    private val invoiceDao = database.invoiceDao()
    private val voucherDao = database.voucherDao()

    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allInvoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    fun getCustomer(id: Long): Flow<Customer?> = customerDao.getCustomerById(id)
    fun getInvoice(id: Long): Flow<Invoice?> = invoiceDao.getInvoiceById(id)
    fun getInvoiceItems(invoiceId: Long): Flow<List<InvoiceItem>> = invoiceDao.getItemsForInvoice(invoiceId)
    suspend fun getInvoiceItemsSync(invoiceId: Long): List<InvoiceItem> = invoiceDao.getItemsForInvoiceSync(invoiceId)
    fun getCustomerVouchers(customerId: Long): Flow<List<VoucherTransaction>> = voucherDao.getVouchersForCustomer(customerId)

    suspend fun addCustomer(name: String, phone: String, openingBalance: Double, notes: String): Long {
        val customer = Customer(
            name = name.trim(),
            phone = phone.trim(),
            openingBalance = openingBalance,
            currentBalance = openingBalance,
            notes = notes.trim()
        )
        val customerId = customerDao.insertCustomer(customer)
        if (openingBalance != 0.0) {
            val isDebit = openingBalance > 0
            voucherDao.insertVoucher(
                VoucherTransaction(
                    customerId = customerId,
                    voucherType = if (isDebit) VoucherType.PAYMENT else VoucherType.RECEIPT,
                    amount = kotlin.math.abs(openingBalance),
                    debit = if (isDebit) openingBalance else 0.0,
                    credit = if (!isDebit) kotlin.math.abs(openingBalance) else 0.0,
                    runningBalance = openingBalance,
                    statement = "رصيد افتتاحي"
                )
            )
        }
        return customerId
    }

    suspend fun updateCustomer(customer: Customer) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customer: Customer) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun getNextInvoiceNumber(): String {
        val count = invoiceDao.getInvoiceCount() + 1
        return String.format(java.util.Locale.US, "INV-%04d", count)
    }

    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>): Long {
        val invoiceId = invoiceDao.insertInvoice(invoice)
        val itemsWithId = items.map { it.copy(invoiceId = invoiceId) }
        invoiceDao.insertInvoiceItems(itemsWithId)

        // If Credit invoice linked to a customer, update customer balance & create voucher
        if (invoice.paymentType == PaymentType.CREDIT && invoice.customerId != null) {
            val customer = customerDao.getCustomerByIdSync(invoice.customerId)
            if (customer != null) {
                val creditDebt = invoice.grandTotal - invoice.paidAmount
                val newBalance = customer.currentBalance + creditDebt
                voucherDao.insertVoucher(
                    VoucherTransaction(
                        customerId = customer.id,
                        voucherType = VoucherType.INVOICE_CREDIT,
                        amount = creditDebt,
                        debit = creditDebt,
                        credit = 0.0,
                        runningBalance = newBalance,
                        statement = "فاتورة مبيعات آجلة رقم ${invoice.invoiceNumber}",
                        invoiceId = invoiceId
                    )
                )
                customerDao.updateCustomerBalance(customer.id, newBalance)
            }
        }
        return invoiceId
    }

    suspend fun addVoucher(
        customerId: Long,
        type: VoucherType,
        amount: Double,
        statement: String
    ): Long {
        val customer = customerDao.getCustomerByIdSync(customerId) ?: return -1
        val currentBalance = customer.currentBalance

        val (debit, credit, newBalance) = when (type) {
            VoucherType.RECEIPT -> {
                // Receipt voucher (قبض): Customer pays merchant -> decreases customer's debt
                Triple(0.0, amount, currentBalance - amount)
            }
            VoucherType.PAYMENT -> {
                // Payment voucher (صرف): Merchant pays customer / refund -> increases customer's debt
                Triple(amount, 0.0, currentBalance + amount)
            }
            VoucherType.INVOICE_CREDIT -> {
                Triple(amount, 0.0, currentBalance + amount)
            }
        }

        val voucher = VoucherTransaction(
            customerId = customerId,
            voucherType = type,
            amount = amount,
            debit = debit,
            credit = credit,
            runningBalance = newBalance,
            statement = statement.ifBlank { type.labelAr }
        )

        val voucherId = voucherDao.insertVoucher(voucher)
        customerDao.updateCustomerBalance(customerId, newBalance)
        return voucherId
    }

    suspend fun seedInitialDataIfNeeded() {
        val existing = customerDao.getAllCustomers().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val c1Id = addCustomer(
                name = "مؤسسة الأمل التجارية",
                phone = "770123456",
                openingBalance = 150000.0,
                notes = "عميل جملة رئيسي"
            )
            val c2Id = addCustomer(
                name = "شركة النجم للتوزيع",
                phone = "771987654",
                openingBalance = 85000.0,
                notes = "منطقة التحرير"
            )
            val c3Id = addCustomer(
                name = "محل البركة للمواد الغذائية",
                phone = "773344556",
                openingBalance = 0.0,
                notes = "سداد دوري نقدي"
            )

            // Seed sample invoice
            val sampleInvoice = Invoice(
                invoiceNumber = "INV-0001",
                customerId = c1Id,
                customerName = "مؤسسة الأمل التجارية",
                paymentType = PaymentType.CREDIT,
                subtotal = 75000.0,
                discount = 0.0,
                tax = 0.0,
                grandTotal = 75000.0,
                paidAmount = 0.0,
                remainingAmount = 75000.0,
                notes = "بضاعة موردة للفرع"
            )
            val sampleItems = listOf(
                InvoiceItem(description = "سكر برازيلي 50 كجم", quantity = 2.0, unitPrice = 25000.0, totalPrice = 50000.0),
                InvoiceItem(description = "أرز تايلندي درجة أولى 20 كجم", quantity = 1.0, unitPrice = 25000.0, totalPrice = 25000.0)
            )
            createInvoice(sampleInvoice, sampleItems)
        }
    }
}
