package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VoucherTransaction
import com.example.data.model.VoucherType
import com.example.service.EscPos58mmPrinterService
import com.example.ui.theme.CoralDebit
import com.example.ui.theme.GreenCredit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RunningBalanceCard(
    voucher: VoucherTransaction,
    modifier: Modifier = Modifier
) {
    val dateStr = rememberDate(voucher.createdAt)
    val (typeText, typeColor) = when (voucher.voucherType) {
        VoucherType.RECEIPT -> "سند قبض" to GreenCredit
        VoucherType.PAYMENT -> "سند صرف" to CoralDebit
        VoucherType.INVOICE_CREDIT -> "فاتورة آجلة" to CoralDebit
    }
    val balance = voucher.runningBalance
    val balanceLabel = when {
        balance > 0 -> "عليه"
        balance < 0 -> "له"
        else -> "خالص"
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            LedgerRow("التاريخ", dateStr, emphasize = false)
            LedgerDivider()
            LedgerRow("نوع العملية", typeText, valueColor = typeColor)
            LedgerDivider()
            LedgerRow("مبلغ العملية", "${EscPos58mmPrinterService.formatAmount(voucher.amount)} ر.ي",
                valueColor = if (voucher.credit > 0) GreenCredit else CoralDebit, emphasize = true)
            LedgerDivider()
            LedgerRow("التفاصيل", voucher.statement.ifBlank { "—" }, emphasize = false)
            LedgerDivider()
            LedgerRow(
                "الرصيد التراكمي",
                "${EscPos58mmPrinterService.formatAmount(kotlin.math.abs(balance))} ر.ي ($balanceLabel)",
                valueColor = if (balance > 0) CoralDebit else if (balance < 0) GreenCredit else MaterialTheme.colorScheme.onSurface,
                emphasize = true
            )
        }
    }
}

@Composable
private fun LedgerRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.85f),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.5f),
            fontSize = if (emphasize) 13.sp else 12.sp,
            color = valueColor,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun LedgerDivider() {
    Spacer(
        modifier = Modifier.fillMaxWidth().height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f))
    )
}

@Composable
private fun rememberDate(timestamp: Long): String {
    return androidx.compose.runtime.remember(timestamp) {
        SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale.US).format(Date(timestamp))
    }
}
