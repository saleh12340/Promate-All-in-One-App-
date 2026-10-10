package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.PaymentType
import com.example.data.model.VoucherTransaction
import com.example.data.model.VoucherType
import com.example.data.repository.AccountingRepository
import com.example.service.AudioTranscriptionService
import com.example.service.EscPos58mmPrinterService
import com.example.service.PdfReceiptExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class InvoiceItemDraft(
    val id: String = UUID.randomUUID().toString(),
    val description: String = "",
    val quantityStr: String = "1",
    val unitPriceStr: String = ""
) {
    val quantity: Double
        get() = quantityStr.toDoubleOrNull() ?: 0.0

    val unitPrice: Double
        get() = unitPriceStr.toDoubleOrNull() ?: 0.0

    val totalPrice: Double
        get() = quantity * unitPrice
}

sealed class VoiceTarget {
    data class InvoiceItemDescription(val index: Int) : VoiceTarget()
    object VoucherStatement : VoiceTarget()
    object InvoiceNotes : VoiceTarget()
    object CustomerName : VoiceTarget()
}

class AccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = AccountingRepository(database)
    val audioService = AudioTranscriptionService(application)

    // Data streams
    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<Invoice>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Customer & Ledger
    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _customerVouchers = MutableStateFlow<List<VoucherTransaction>>(emptyList())
    val customerVouchers: StateFlow<List<VoucherTransaction>> = _customerVouchers.asStateFlow()

    // Single-Row Action Bar State (سند قبض / سند صرف)
    val voucherType = MutableStateFlow(VoucherType.RECEIPT)
    val voucherAmount = MutableStateFlow("")
    val voucherStatement = MutableStateFlow("")
    val isVoucherSaving = MutableStateFlow(false)

    // Invoicing Engine State
    val invoiceNumber = MutableStateFlow("INV-0001")
    val invoiceCustomer = MutableStateFlow<Customer?>(null)
    val invoiceCustomerName = MutableStateFlow("")
    val paymentType = MutableStateFlow(PaymentType.CASH)
    val invoiceNotes = MutableStateFlow("")
    val paidAmountStr = MutableStateFlow("")

    // Auto-expanding rows: starts with 2 default rows
    private val _invoiceRows = MutableStateFlow(
        listOf(
            InvoiceItemDraft(),
            InvoiceItemDraft()
        )
    )
    val invoiceRows: StateFlow<List<InvoiceItemDraft>> = _invoiceRows.asStateFlow()

    // Voice transcription state
    val isRecording = MutableStateFlow(false)
    val isTranscribing = MutableStateFlow(false)
    val activeVoiceTarget = MutableStateFlow<VoiceTarget?>(null)

    // Printing and Export State
    val previewReceiptBitmap = MutableStateFlow<Bitmap?>(null)
    val showReceiptDialog = MutableStateFlow(false)
    val currentInvoiceForPrint = MutableStateFlow<Invoice?>(null)
    val currentItemsForPrint = MutableStateFlow<List<InvoiceItem>>(emptyList())

    val storeName = MutableStateFlow("مؤسسة الرواد التجارية")
    val storePhone = MutableStateFlow("777000111")

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            invoiceNumber.value = repository.getNextInvoiceNumber()
        }
    }

    // --- Customer Selection & Ledger ---
    fun selectCustomer(customer: Customer?) {
        _selectedCustomerId.value = customer?.id
        _selectedCustomer.value = customer
        if (customer != null) {
            viewModelScope.launch {
                repository.getCustomerVouchers(customer.id).collect { list ->
                    _customerVouchers.value = list
                }
            }
        } else {
            _customerVouchers.value = emptyList()
        }
    }

    fun addCustomer(name: String, phone: String, openingBalance: Double, notes: String, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.addCustomer(name, phone, openingBalance, notes)
            onDone(id)
        }
    }

    // --- Single-Row Action Bar for Customer Ledger ---
    fun submitVoucherFromActionBar() {
        val customer = _selectedCustomer.value ?: return
        val amount = voucherAmount.value.toDoubleOrNull() ?: return
        if (amount <= 0) return

        viewModelScope.launch {
            isVoucherSaving.value = true
            val stmt = voucherStatement.value.ifBlank { voucherType.value.labelAr }
            repository.addVoucher(
                customerId = customer.id,
                type = voucherType.value,
                amount = amount,
                statement = stmt
            )
            // refresh customer object
            val updated = database.customerDao().getCustomerByIdSync(customer.id)
            _selectedCustomer.value = updated

            voucherAmount.value = ""
            voucherStatement.value = ""
            isVoucherSaving.value = false
            Toast.makeText(getApplication(), "تم إضافة ${voucherType.value.labelAr} بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Interactive Invoicing Engine & Auto-Expanding Rows ---
    fun updateInvoiceRow(index: Int, description: String? = null, quantityStr: String? = null, unitPriceStr: String? = null) {
        val currentList = _invoiceRows.value.toMutableList()
        if (index !in currentList.indices) return

        val oldItem = currentList[index]
        val newItem = oldItem.copy(
            description = description ?: oldItem.description,
            quantityStr = quantityStr ?: oldItem.quantityStr,
            unitPriceStr = unitPriceStr ?: oldItem.unitPriceStr
        )
        currentList[index] = newItem

        // Auto-Expanding Rows logic:
        // If the user modified the last row and it has non-blank description or unitPrice, automatically append a new row
        if (index == currentList.size - 1 && (newItem.description.isNotBlank() || newItem.unitPrice > 0)) {
            currentList.add(InvoiceItemDraft())
        }

        _invoiceRows.value = currentList
    }

    fun removeInvoiceRow(index: Int) {
        val currentList = _invoiceRows.value.toMutableList()
        if (currentList.size > 1 && index in currentList.indices) {
            currentList.removeAt(index)
            // Ensure at least 2 rows
            while (currentList.size < 2) {
                currentList.add(InvoiceItemDraft())
            }
            _invoiceRows.value = currentList
        }
    }

    fun resetInvoiceForm() {
        viewModelScope.launch {
            invoiceNumber.value = repository.getNextInvoiceNumber()
            invoiceCustomer.value = null
            invoiceCustomerName.value = ""
            paymentType.value = PaymentType.CASH
            invoiceNotes.value = ""
            paidAmountStr.value = ""
            _invoiceRows.value = listOf(InvoiceItemDraft(), InvoiceItemDraft())
        }
    }

    fun saveInvoice(onSuccess: (Long) -> Unit) {
        val validItems = _invoiceRows.value.filter { it.description.isNotBlank() && it.totalPrice > 0 }
        if (validItems.isEmpty()) {
            Toast.makeText(getApplication(), "يرجى إدخال صنف واحد على الأقل", Toast.LENGTH_SHORT).show()
            return
        }

        val cust = invoiceCustomer.value
        val custName = cust?.name ?: invoiceCustomerName.value.trim().ifBlank { "عميل نقدي" }
        val grandTotal = validItems.sumOf { it.totalPrice }
        val paid = if (paymentType.value == PaymentType.CASH) grandTotal else (paidAmountStr.value.toDoubleOrNull() ?: 0.0)
        val remaining = (grandTotal - paid).coerceAtLeast(0.0)

        val invoice = Invoice(
            invoiceNumber = invoiceNumber.value,
            customerId = cust?.id,
            customerName = custName,
            paymentType = paymentType.value,
            subtotal = grandTotal,
            discount = 0.0,
            tax = 0.0,
            grandTotal = grandTotal,
            paidAmount = paid,
            remainingAmount = remaining,
            notes = invoiceNotes.value
        )

        val invoiceItemList = validItems.map {
            InvoiceItem(
                description = it.description.trim(),
                quantity = it.quantity,
                unitPrice = it.unitPrice,
                totalPrice = it.totalPrice
            )
        }

        viewModelScope.launch {
            val invoiceId = repository.createInvoice(invoice, invoiceItemList)
            currentInvoiceForPrint.value = invoice.copy(id = invoiceId)
            currentItemsForPrint.value = invoiceItemList

            // Render 58mm preview
            val bmp = EscPos58mmPrinterService.renderReceiptBitmap(
                storeName = storeName.value,
                storePhone = storePhone.value,
                invoice = invoice,
                items = invoiceItemList
            )
            previewReceiptBitmap.value = bmp
            showReceiptDialog.value = true

            resetInvoiceForm()
            onSuccess(invoiceId)
        }
    }

    // --- Voice Recording & Gemini Transcription ---
    fun startVoiceInput(target: VoiceTarget) {
        activeVoiceTarget.value = target
        val started = audioService.startRecording()
        if (started) {
            isRecording.value = true
        } else {
            Toast.makeText(getApplication(), "تعذر بدء التسجيل، تأكد من إذن الميكروفون", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopVoiceInputAndTranscribe() {
        val target = activeVoiceTarget.value
        isRecording.value = false
        val audioFile = audioService.stopRecording()
        if (audioFile == null || target == null) return

        viewModelScope.launch {
            isTranscribing.value = true
            val result = audioService.transcribeAudio(audioFile)
            isTranscribing.value = false
            activeVoiceTarget.value = null

            result.onSuccess { transcribedText ->
                applyTranscribedText(target, transcribedText)
                Toast.makeText(getApplication(), "تم التفريغ: $transcribedText", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(getApplication(), "خطأ بالتفريغ: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun cancelVoiceInput() {
        audioService.cancelRecording()
        isRecording.value = false
        isTranscribing.value = false
        activeVoiceTarget.value = null
    }

    private fun applyTranscribedText(target: VoiceTarget, text: String) {
        val clean = text.trim()
        when (target) {
            is VoiceTarget.InvoiceItemDescription -> {
                updateInvoiceRow(target.index, description = clean)
            }
            VoiceTarget.VoucherStatement -> {
                voucherStatement.value = clean
            }
            VoiceTarget.InvoiceNotes -> {
                invoiceNotes.value = if (invoiceNotes.value.isBlank()) clean else "${invoiceNotes.value} $clean"
            }
            VoiceTarget.CustomerName -> {
                invoiceCustomerName.value = clean
            }
        }
    }

    // --- Thermal Print & PDF Export ---
    fun openReceiptForInvoice(invoice: Invoice) {
        viewModelScope.launch {
            val items = repository.getInvoiceItemsSync(invoice.id)
            currentInvoiceForPrint.value = invoice
            currentItemsForPrint.value = items

            val bmp = EscPos58mmPrinterService.renderReceiptBitmap(
                storeName = storeName.value,
                storePhone = storePhone.value,
                invoice = invoice,
                items = items
            )
            previewReceiptBitmap.value = bmp
            showReceiptDialog.value = true
        }
    }

    fun generatePdf(invoice: Invoice, items: List<InvoiceItem>): File {
        return PdfReceiptExporter.generateInvoicePdf(
            context = getApplication(),
            storeName = storeName.value,
            storePhone = storePhone.value,
            invoice = invoice,
            items = items
        )
    }
}
