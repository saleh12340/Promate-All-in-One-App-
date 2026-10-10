package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.Customer
import com.example.data.model.PaymentType
import com.example.service.EscPos58mmPrinterService
import com.example.ui.components.InvoiceTable
import com.example.ui.theme.CoralDebit
import com.example.ui.theme.GreenCredit
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.VoiceTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicingScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val invoiceNumber by viewModel.invoiceNumber.collectAsState()
    val selectedCustomer by viewModel.invoiceCustomer.collectAsState()
    val customerNameText by viewModel.invoiceCustomerName.collectAsState()
    val paymentType by viewModel.paymentType.collectAsState()
    val rows by viewModel.invoiceRows.collectAsState()
    val notes by viewModel.invoiceNotes.collectAsState()
    val paidAmountStr by viewModel.paidAmountStr.collectAsState()

    var showCustomerDialog by remember { mutableStateOf(false) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "إذن الميكروفون مطلوب للإملاء الصوتي", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestVoiceTarget(target: VoiceTarget) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceInput(target)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val grandTotal = rows.filter { it.description.isNotBlank() && it.totalPrice > 0 }.sumOf { it.totalPrice }
    val paid = if (paymentType == PaymentType.CASH) grandTotal else (paidAmountStr.toDoubleOrNull() ?: 0.0)
    val remaining = (grandTotal - paid).coerceAtLeast(0.0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("invoicing_screen")
    ) {
        // TOP OF SCREEN UTILIZATION: Top Header & Invoice Controls Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Top row: Invoice #, Date, Reset Form
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "فاتورة: $invoiceNumber",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "نظام الفواتير الذكي",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { viewModel.resetInvoiceForm() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "تفريغ الفاتورة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Type Selector (نقدي / آجل)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = paymentType == PaymentType.CASH,
                        onClick = { viewModel.paymentType.value = PaymentType.CASH },
                        label = { Text("نقدي (كاش)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (paymentType == PaymentType.CASH) GreenCredit else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenCredit.copy(alpha = 0.15f),
                            selectedLabelColor = GreenCredit
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = paymentType == PaymentType.CREDIT,
                        onClick = { viewModel.paymentType.value = PaymentType.CREDIT },
                        label = { Text("آجل (حساب عميل)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (paymentType == PaymentType.CREDIT) CoralDebit else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralDebit.copy(alpha = 0.15f),
                            selectedLabelColor = CoralDebit
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Customer Selection Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = customerDropdownExpanded,
                        onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: customerNameText,
                            onValueChange = {
                                viewModel.invoiceCustomer.value = null
                                viewModel.invoiceCustomerName.value = it
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("invoice_customer_input"),
                            placeholder = { Text("اختر العميل أو اكتب الاسم...", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("عميل نقدي عام", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.invoiceCustomer.value = null
                                    viewModel.invoiceCustomerName.value = "عميل نقدي"
                                    customerDropdownExpanded = false
                                }
                            )
                            customers.forEach { cust ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(cust.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            val bal = cust.currentBalance
                                            Text(
                                                text = "الرصيد: ${EscPos58mmPrinterService.formatAmount(bal)} ر.ي",
                                                fontSize = 11.sp,
                                                color = if (bal > 0) CoralDebit else GreenCredit
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.invoiceCustomer.value = cust
                                        viewModel.invoiceCustomerName.value = cust.name
                                        customerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Quick Add Customer Button
                    IconButton(
                        onClick = { showCustomerDialog = true },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "إضافة عميل جديد",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Voice Input Button for Customer Name
                    IconButton(
                        onClick = { requestVoiceTarget(VoiceTarget.CustomerName) },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "إملاء اسم العميل",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // INTERACTIVE INVOICING TABLE (Auto-expanding rows, clean quantity, text wrapping)
        Text(
            text = "جدول الأصناف (تمدد تلقائي للأسطر):",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        InvoiceTable(
            rows = rows,
            onUpdateRow = { index, desc, qty, price ->
                viewModel.updateInvoiceRow(index, desc, qty, price)
            },
            onRemoveRow = { index ->
                viewModel.removeInvoiceRow(index)
            },
            onVoiceClick = { index ->
                requestVoiceTarget(VoiceTarget.InvoiceItemDescription(index))
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        // TOTALS & PAYMENT SETTINGS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Total Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الإجمالي الكلي للفاتورة:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${EscPos58mmPrinterService.formatAmount(grandTotal)} ر.ي",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // If Credit, show Paid and Remaining inputs
                AnimatedVisibility(visible = paymentType == PaymentType.CREDIT) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = paidAmountStr,
                                onValueChange = { viewModel.paidAmountStr.value = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("المدفوع نقداً (عربون)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = CoralDebit.copy(alpha = 0.12f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("المتبقي الآجل:", fontSize = 10.sp, color = CoralDebit)
                                    Text(
                                        text = "${EscPos58mmPrinterService.formatAmount(remaining)} ر.ي",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralDebit
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notes Field with Voice Dictation
                OutlinedTextField(
                    value = notes,
                    onValueChange = { viewModel.invoiceNotes.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ملاحظات الفاتورة أو شروط التسليم...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        IconButton(onClick = { requestVoiceTarget(VoiceTarget.InvoiceNotes) }) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "صوت",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Action Button: Save & Print 58mm Thermal
                Button(
                    onClick = {
                        viewModel.saveInvoice {
                            Toast.makeText(context, "تم حفظ الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_invoice_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "حفظ وإصدار الفاتورة (طباعة 58mm)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Add Customer Quick Dialog
    if (showCustomerDialog) {
        QuickAddCustomerDialog(
            onDismiss = { showCustomerDialog = false },
            onAdd = { name, phone, openingBalance, notesText ->
                viewModel.addCustomer(name, phone, openingBalance, notesText) { id ->
                    val newCust = Customer(
                        id = id,
                        name = name,
                        phone = phone,
                        openingBalance = openingBalance,
                        currentBalance = openingBalance
                    )
                    viewModel.invoiceCustomer.value = newCust
                    viewModel.invoiceCustomerName.value = name
                    showCustomerDialog = false
                }
            }
        )
    }
}

@Composable
fun QuickAddCustomerDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, openingBalance: Double, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var openingBalance by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "إضافة عميل جديد",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم العميل *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = openingBalance,
                    onValueChange = { openingBalance = it },
                    label = { Text("الرصيد الافتتاحي (ر.ي)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onAdd(name, phone, openingBalance.toDoubleOrNull() ?: 0.0, notes)
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("إضافة")
                    }
                }
            }
        }
    }
}
