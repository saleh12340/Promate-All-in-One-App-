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
        get() = quantityStr.toDoubleOrNull() ?: 1.0

    val unitPrice: Double
        get() = unitPriceStr.toDoubleOrNull() ?: 0.0

    val totalPrice: Double
        get() = quantity * unitPrice
}

sealed class VoiceTarget {
    data class InvoiceItemDescription(val index: Int) : VoiceTarget()
    object TopItemDetails : VoiceTarget()
    object VoucherStatement : VoiceTarget()
    object InvoiceNotes : VoiceTarget()
    object CustomerName : VoiceTarget()
}

class AccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = AccountingRepository(database)
    val audioService = AudioTranscriptionService(application)

    // Store profile info
    val storeName = MutableStateFlow("بقالة العزي")
    val storePhone = MutableStateFlow("776425052")

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

    // Top Input Section Fields (سعر الواحدة, التفاصيل, العدد, القيمة الإجمالية)
    val topItemUnitPrice = MutableStateFlow("")
    val topItemDetails = MutableStateFlow("")
    val topItemQty = MutableStateFlow("1")
    val topItemTotal = MutableStateFlow("")

    // Invoicing Engine State
    val invoiceNumber = MutableStateFlow("1118")
    val invoiceCustomer = MutableStateFlow<Customer?>(null)
    val invoiceCustomerName = MutableStateFlow("سليمان البرعسي")
    val paymentType = MutableStateFlow(PaymentType.CASH)
    val invoiceNotes = MutableStateFlow("")
    val paidAmountStr = MutableStateFlow("")

    // Auto-expanding rows list: initialized with realistic demo items from screenshot
    private val _invoiceRows = MutableStateFlow(
        listOf(
            InvoiceItemDraft(description = "كيس بر", quantityStr = "1", unitPriceStr = "13500"),
            InvoiceItemDraft(description = "احذي حمام", quantityStr = "1", unitPriceStr = "800"),
            InvoiceItemDraft(description = "خميره كبير", quantityStr = "1", unitPriceStr = "900"),
            InvoiceItemDraft(description = "شحم طبي كبير", quantityStr = "1", unitPriceStr = "1000"),
            InvoiceItemDraft(description = "نص سكر", quantityStr = "1", unitPriceStr = "10900"),
            InvoiceItemDraft(description = "قشر", quantityStr = "1", unitPriceStr = "1000"),
            InvoiceItemDraft(description = "حلبه", quantityStr = "1", unitPriceStr = "300"),
            InvoiceItemDraft(description = "نص صلصه", quantityStr = "1", unitPriceStr = "1300"),
            InvoiceItemDraft(description = "شاهي ربع", quantityStr = "2", unitPriceStr = "900"),
            InvoiceItemDraft(description = "دبه زيت", quantityStr = "1", unitPriceStr = "4800"),
            InvoiceItemDraft(description = "قطمه رز عايدي", quantityStr = "1", unitPriceStr = "3800"),
            InvoiceItemDraft(description = "سمن الماس", quantityStr = "1", unitPriceStr = "1700"),
            InvoiceItemDraft(description = "حوايج وهرد", quantityStr = "1", unitPriceStr = "1400"),
            InvoiceItemDraft(description = "كفف و2كوافي", quantityStr = "1", unitPriceStr = "1200"),
            InvoiceItemDraft(description = "قطمه رز بسمتي", quantityStr = "1", unitPriceStr = "4000"),
            InvoiceItemDraft(description = "مقوش عاج", quantityStr = "1", unitPriceStr = "500"),
            InvoiceItemDraft(description = "قطمه صابون", quantityStr = "1", unitPriceStr = "4700")
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

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            val nextNum = repository.getNextInvoiceNumber().replace("INV-", "")
            if (nextNum.isNotBlank()) {
                invoiceNumber.value = nextNum
            }
        }
    }

    // Add Item from the 4 top boxes into invoice table
    fun addTopItemToInvoice() {
        val details = topItemDetails.value.trim()
        val unitPrice = topItemUnitPrice.value.toDoubleOrNull() ?: 0.0
        val qty = topItemQty.value.toDoubleOrNull() ?: 1.0

        if (details.isBlank()) {
            Toast.makeText(getApplication(), "يرجى كتابة تفاصيل / اسم الصنف", Toast.LENGTH_SHORT).show()
            return
        }

        val newItem = InvoiceItemDraft(
            description = details,
            quantityStr = if (qty % 1.0 == 0.0) String.format(java.util.Locale.US, "%.0f", qty) else qty.toString(),
            unitPriceStr = if (unitPrice > 0) String.format(java.util.Locale.US, "%.0f", unitPrice) else ""
        )

        val currentList = _invoiceRows.value.toMutableList()
        currentList.add(newItem)
        _invoiceRows.value = currentList

        // Clear top fields for next quick entry
        topItemDetails.value = ""
        topItemUnitPrice.value = ""
        topItemQty.value = "1"
        topItemTotal.value = ""

        Toast.makeText(getApplication(), "تمت إضافة الصنف: $details", Toast.LENGTH_SHORT).show()
    }

    // Auto calculate top total when price or qty changes
    fun onTopUnitPriceChanged(price: String) {
        topItemUnitPrice.value = price
        val p = price.toDoubleOrNull() ?: 0.0
        val q = topItemQty.value.toDoubleOrNull() ?: 1.0
        val total = p * q
        topItemTotal.value = if (total > 0) EscPos58mmPrinterService.formatAmount(total) else ""
    }

    fun onTopQtyChanged(qty: String) {
        topItemQty.value = qty
        val p = topItemUnitPrice.value.toDoubleOrNull() ?: 0.0
        val q = qty.toDoubleOrNull() ?: 1.0
        val total = p * q
        topItemTotal.value = if (total > 0) EscPos58mmPrinterService.formatAmount(total) else ""
    }

    fun onTopTotalChanged(totalStr: String) {
        topItemTotal.value = totalStr
        val t = totalStr.replace(",", "").toDoubleOrNull() ?: 0.0
        val q = topItemQty.value.toDoubleOrNull() ?: 1.0
        if (q > 0) {
            val p = t / q
            topItemUnitPrice.value = if (p > 0) String.format(java.util.Locale.US, "%.0f", p) else ""
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
            val updated = database.customerDao().getCustomerByIdSync(customer.id)
            _selectedCustomer.value = updated

            voucherAmount.value = ""
            voucherStatement.value = ""
            isVoucherSaving.value = false
            Toast.makeText(getApplication(), "تم إضافة ${voucherType.value.labelAr} بنجاح", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Table Rows Management ---
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

        if (index == currentList.size - 1 && (newItem.description.isNotBlank() || newItem.unitPrice > 0)) {
            currentList.add(InvoiceItemDraft())
        }

        _invoiceRows.value = currentList
    }

    fun removeInvoiceRow(index: Int) {
        val currentList = _invoiceRows.value.toMutableList()
        if (index in currentList.indices) {
            currentList.removeAt(index)
            if (currentList.isEmpty()) {
                currentList.add(InvoiceItemDraft())
            }
            _invoiceRows.value = currentList
        }
    }

    fun resetInvoiceForm() {
        viewModelScope.launch {
            val count = repository.getNextInvoiceNumber().replace("INV-", "")
            invoiceNumber.value = count.ifBlank { "1119" }
            invoiceCustomer.value = null
            invoiceCustomerName.value = "سليمان البرعسي"
            paymentType.value = PaymentType.CASH
            invoiceNotes.value = ""
            paidAmountStr.value = ""
            _invoiceRows.value = listOf(
                InvoiceItemDraft(description = "", quantityStr = "1", unitPriceStr = ""),
                InvoiceItemDraft(description = "", quantityStr = "1", unitPriceStr = "")
            )
            topItemDetails.value = ""
            topItemUnitPrice.value = ""
            topItemQty.value = "1"
            topItemTotal.value = ""
            Toast.makeText(getApplication(), "تم إنشاء فاتورة جديدة", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveInvoice(onSuccess: (Long) -> Unit) {
        val validItems = _invoiceRows.value.filter { it.description.isNotBlank() && it.totalPrice > 0 }
        if (validItems.isEmpty()) {
            Toast.makeText(getApplication(), "يرجى إضافة صنف واحد على الأقل", Toast.LENGTH_SHORT).show()
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

            val bmp = EscPos58mmPrinterService.renderReceiptBitmap(
                storeName = storeName.value,
                storePhone = storePhone.value,
                invoice = invoice,
                items = invoiceItemList
            )
            previewReceiptBitmap.value = bmp
            showReceiptDialog.value = true

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
            VoiceTarget.TopItemDetails -> {
                topItemDetails.value = clean
            }
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
}
