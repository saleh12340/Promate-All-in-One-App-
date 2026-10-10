package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Invoice
import com.example.data.model.PaymentType
import com.example.service.EscPos58mmPrinterService
import com.example.ui.theme.CoralDebit
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GreenCredit
import com.example.ui.viewmodel.AccountingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    onNavigateToInvoicing: () -> Unit,
    onNavigateToLedger: () -> Unit,
    onNavigateToInvoices: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    val invoices by viewModel.invoices.collectAsState()

    val totalSales = invoices.sumOf { it.grandTotal }
    val totalCustomerDebt = customers.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 10.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "المحاسب الذكي Enterprise",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "إدارة المبيعات • سندات القبض والصرف • طباعة 58mm",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        // Summary KPI Cards (Sales & Debt in Yemeni Rial)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "إجمالي المبيعات",
                    value = "${EscPos58mmPrinterService.formatAmount(totalSales)} ر.ي",
                    subtitle = "${invoices.size} فاتورة مصدرة",
                    icon = Icons.Default.AttachMoney,
                    color = GreenCredit,
                    modifier = Modifier.weight(1f)
                )

                KpiCard(
                    title = "إجمالي الديون القائمة",
                    value = "${EscPos58mmPrinterService.formatAmount(totalCustomerDebt)} ر.ي",
                    subtitle = "${customers.count { it.currentBalance > 0 }} عميل مدين",
                    icon = Icons.Default.ReceiptLong,
                    color = CoralDebit,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Quick Navigation Grid
        item {
            Text(
                text = "الوصول السريع للعمليات:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "فاتورة جديدة",
                    desc = "تمدد أسطر + طباعة",
                    icon = Icons.Default.AddShoppingCart,
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToInvoicing,
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    title = "كشف حساب",
                    desc = "شريط سندات سريع",
                    icon = Icons.Default.MenuBook,
                    color = GreenCredit,
                    onClick = onNavigateToLedger,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "سجل الفواتير",
                    desc = "طباعة ومشاركة PDF",
                    icon = Icons.Default.Print,
                    color = GoldAccent,
                    onClick = onNavigateToInvoices,
                    modifier = Modifier.weight(1f)
                )

                QuickActionCard(
                    title = "حسابات العملاء",
                    desc = "${customers.size} عميل مسجل",
                    icon = Icons.Default.Groups,
                    color = CoralDebit,
                    onClick = onNavigateToCustomers,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Invoices Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر الفواتير المصدرة:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "عرض الكل",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToInvoices() }
                )
            }
        }

        items(invoices.take(5), key = { it.id }) { invoice ->
            RecentInvoiceRow(
                invoice = invoice,
                onClick = { viewModel.openReceiptForInvoice(invoice) }
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RecentInvoiceRow(
    invoice: Invoice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
    val dateStr = dateFormat.format(Date(invoice.createdAt))

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = invoice.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (invoice.paymentType == PaymentType.CASH) GreenCredit.copy(alpha = 0.15f) else CoralDebit.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = invoice.paymentType.labelAr,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (invoice.paymentType == PaymentType.CASH) GreenCredit else CoralDebit,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${invoice.customerName.ifBlank { "عميل نقدي" }} • $dateStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${EscPos58mmPrinterService.formatAmount(invoice.grandTotal)} ر.ي",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "طباعة",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
