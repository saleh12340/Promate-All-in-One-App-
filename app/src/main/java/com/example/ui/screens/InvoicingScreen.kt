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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.PaymentType
import com.example.service.EscPos58mmPrinterService
import com.example.ui.components.DualColumnInvoiceTable
import com.example.ui.theme.CellBorderColor
import com.example.ui.theme.CoralRed
import com.example.ui.theme.DarkText
import com.example.ui.theme.LightBluePill
import com.example.ui.theme.NavyHeader
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.TextBlue
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.VoiceTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicingScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storeName by viewModel.storeName.collectAsState()
    val storePhone by viewModel.storePhone.collectAsState()
    val invoiceNumber by viewModel.invoiceNumber.collectAsState()
    val customerName by viewModel.invoiceCustomerName.collectAsState()
    val paymentType by viewModel.paymentType.collectAsState()
    val rows by viewModel.invoiceRows.collectAsState()

    // 4 Top input boxes
    val topUnitPrice by viewModel.topItemUnitPrice.collectAsState()
    val topDetails by viewModel.topItemDetails.collectAsState()
    val topQty by viewModel.topItemQty.collectAsState()
    val topTotal by viewModel.topItemTotal.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "إذن الميكروفون مطلوب للإملاء الصوتي", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestVoiceForDetails() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceInput(VoiceTarget.TopItemDetails)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val grandTotal = rows.filter { it.description.isNotBlank() && it.totalPrice > 0 }.sumOf { it.totalPrice }
    val currentDateStr = remember {
        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 8.dp, top = 0.dp, end = 8.dp, bottom = 8.dp)
            .testTag("invoicing_screen")
    ) {
        // 1. TOP BAR (3 Circular Icons + Invoice Title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action Icons on Left
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { viewModel.resetInvoiceForm() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "فاتورة جديدة",
                            tint = DarkText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable {
                            viewModel.saveInvoice {
                                Toast.makeText(context, "معاينة الطباعة الحرارية", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "طباعة سريعة",
                            tint = CoralRed,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "أصناف",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Title on Right
            Text(
                text = "فاتورة #$invoiceNumber - $storeName",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = NavyHeader
            )
        }

        // 2. TOP INPUT CARD (4 Rounded Input Boxes + Big Blue Add Button)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CellBorderColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                // Row of 4 input fields: سعر الواحدة | التفاصيل (مع ميكروفون) | 1 (العدد) | القيمة الإجمالية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // سعر الواحدة
                    OutlinedTextField(
                        value = topUnitPrice,
                        onValueChange = { viewModel.onTopUnitPriceChanged(it) },
                        placeholder = { Text("سعر الواحدة", fontSize = 12.sp, textAlign = TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(52.dp)
                            .testTag("top_price_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CellBorderColor,
                            focusedBorderColor = PrimaryBlue,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAFCFF),
                            cursorColor = PrimaryBlue
                        )
                    )

                    // التفاصيل (with Voice Mic)
                    OutlinedTextField(
                        value = topDetails,
                        onValueChange = { viewModel.topItemDetails.value = it },
                        placeholder = { Text("التفاصيل", fontSize = 13.sp) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = { requestVoiceForDetails() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "صوت",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(52.dp)
                            .testTag("top_details_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CellBorderColor,
                            focusedBorderColor = PrimaryBlue,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAFCFF),
                            cursorColor = PrimaryBlue
                        )
                    )

                    // العدد (1)
                    OutlinedTextField(
                        value = topQty,
                        onValueChange = { viewModel.onTopQtyChanged(it) },
                        placeholder = { Text("1", fontSize = 11.sp, textAlign = TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(0.7f)
                            .height(52.dp)
                            .testTag("top_qty_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CellBorderColor,
                            focusedBorderColor = PrimaryBlue,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAFCFF),
                            cursorColor = PrimaryBlue
                        )
                    )

                    // القيمة الإجمالية
                    OutlinedTextField(
                        value = topTotal,
                        onValueChange = { viewModel.onTopTotalChanged(it) },
                        placeholder = { Text("القيمة الإجمالية", fontSize = 11.sp, textAlign = TextAlign.Center) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp)
                            .testTag("top_total_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CellBorderColor,
                            focusedBorderColor = PrimaryBlue,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAFCFF),
                            cursorColor = PrimaryBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // + إضافة الصنف إلى الفاتورة Button
                Button(
                    onClick = { viewModel.addTopItemToInvoice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("add_item_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إضافة الصنف إلى الفاتورة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. INVOICE HEADER METADATA CARD (Store, Checkboxes, Invoice #, Date, Customer)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CellBorderColor, RoundedCornerShape(10.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                // Top Row: Store Name & Phone | Payment Mode [✓] نقداً [ ] آجل | رقم: 1118 | التاريخ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right: Store Name & Phone
                    Text(
                        text = "$storeName ج: $storePhone",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NavyHeader
                    )

                    // Payment Type Checkboxes: [✓] نقداً  [ ] آجل
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.paymentType.value = PaymentType.CASH }
                        ) {
                            Text(
                                text = if (paymentType == PaymentType.CASH) "[✓] نقداً" else "[ ] نقداً",
                                fontSize = 11.sp,
                                fontWeight = if (paymentType == PaymentType.CASH) FontWeight.Bold else FontWeight.Normal,
                                color = if (paymentType == PaymentType.CASH) PrimaryBlue else DarkText
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { viewModel.paymentType.value = PaymentType.CREDIT }
                        ) {
                            Text(
                                text = if (paymentType == PaymentType.CREDIT) "[✓] آجل" else "[ ] آجل",
                                fontSize = 11.sp,
                                fontWeight = if (paymentType == PaymentType.CREDIT) FontWeight.Bold else FontWeight.Normal,
                                color = if (paymentType == PaymentType.CREDIT) CoralRed else DarkText
                            )
                        }
                    }

                    // Invoice # and Date
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CoralRed.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed)
                        ) {
                            Text(
                                text = "رقم: $invoiceNumber",
                                color = CoralRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "التاريخ: $currentDateStr",
                            fontSize = 10.sp,
                            color = DarkText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Row: Customer Name input & Quick Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { viewModel.invoiceCustomerName.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        placeholder = { Text("اسم العميل", fontSize = 13.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = CellBorderColor,
                            focusedBorderColor = PrimaryBlue,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFFAFCFF),
                            cursorColor = PrimaryBlue
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightBluePill,
                        modifier = Modifier.clickable {
                            // customer selected
                        }
                    ) {
                        Text(
                            text = customerName.ifBlank { "سليمان البرعسي" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. DUAL COLUMN INVOICE TABLE (Matching exact screenshot layout)
        DualColumnInvoiceTable(
            rows = rows,
            onRemoveRow = { viewModel.removeInvoiceRow(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 5. BOTTOM TOTAL & ACTIONS CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CellBorderColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // Grand Total Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${EscPos58mmPrinterService.formatAmount(grandTotal)} ر.ي",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextBlue
                    )

                    Text(
                        text = "المبلغ الإجمالي الكلي:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons Row: [مشاركة] [طباعة حرارية] [حفظ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // مشاركة (Share)
                    OutlinedButton(
                        onClick = {
                            viewModel.saveInvoice {
                                Toast.makeText(context, "مشاركة الفاتورة كصورة أو PDF", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // طباعة حرارية (Thermal Print - Primary Blue Filled)
                    Button(
                        onClick = {
                            viewModel.saveInvoice {
                                Toast.makeText(context, "جاري إرسال أمر الطباعة الحرارية 58mm", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة حرارية", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // حفظ (Save)
                    OutlinedButton(
                        onClick = {
                            viewModel.saveInvoice {
                                Toast.makeText(context, "تم حفظ الفاتورة بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
