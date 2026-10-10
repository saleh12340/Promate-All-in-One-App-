package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.EscPos58mmPrinterService
import com.example.ui.components.RunningBalanceCard
import com.example.ui.components.TopInputActionBar
import com.example.ui.theme.CoralDebit
import com.example.ui.theme.GreenCredit
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.VoiceTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val vouchers by viewModel.customerVouchers.collectAsState()

    val voucherType by viewModel.voucherType.collectAsState()
    val voucherAmount by viewModel.voucherAmount.collectAsState()
    val voucherStatement by viewModel.voucherStatement.collectAsState()
    val isVoucherSaving by viewModel.isVoucherSaving.collectAsState()

    var customerDropdownExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "إذن الميكروفون مطلوب للإملاء الصوتي", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestVoiceForVoucher() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceInput(VoiceTarget.VoucherStatement)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 4.dp)
            .testTag("customer_ledger_screen")
    ) {
        // TOP OF SCREEN UTILIZATION: Single-Row Action Bar for Quick Vouchers
        TopInputActionBar(
            selectedType = voucherType,
            onTypeChange = { viewModel.voucherType.value = it },
            amount = voucherAmount,
            onAmountChange = { viewModel.voucherAmount.value = it },
            statement = voucherStatement,
            onStatementChange = { viewModel.voucherStatement.value = it },
            onVoiceClick = { requestVoiceForVoucher() },
            onSubmit = { viewModel.submitVoucherFromActionBar() },
            isLoading = isVoucherSaving,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Customer Selector & Current Balance Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Customer Selector
                ExposedDropdownMenuBox(
                    expanded = customerDropdownExpanded,
                    onExpandedChange = { customerDropdownExpanded = !customerDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "اختر العميل لعرض كشف الحساب",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded)
                        },
                        shape = MaterialTheme.shapes.small,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        customers.forEach { customer ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(customer.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "${EscPos58mmPrinterService.formatAmount(customer.currentBalance)} ر.ي",
                                            color = if (customer.currentBalance > 0) CoralDebit else GreenCredit,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.selectCustomer(customer)
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedCustomer != null) {
                    val customer = selectedCustomer!!
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (customer.phone.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = customer.phone,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Text("لا يوجد رقم هاتف", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Aggregate Balance Badge
                        val bal = customer.currentBalance
                        val isDebit = bal > 0
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isDebit) CoralDebit.copy(alpha = 0.12f) else GreenCredit.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "الرصيد الكلي: ${EscPos58mmPrinterService.formatAmount(kotlin.math.abs(bal))} ر.ي (${if (isDebit) "مدين" else if (bal < 0) "دائن" else "خالص"})",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDebit) CoralDebit else GreenCredit
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Cumulative Ledger Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "كشف الحساب التراكمي (الحركات المالية):",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${vouchers.size} حركة مسجلة",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (selectedCustomer == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "يرجى اختيار عميل من القائمة أعلاه لعرض كشف الحساب",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        } else if (vouchers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "لا توجد حركات مالية مسجلة لهذا العميل بعد",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(vouchers.reversed(), key = { it.id }) { voucher ->
                    RunningBalanceCard(voucher = voucher)
                }
            }
        }
    }
}
