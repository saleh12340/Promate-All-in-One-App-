package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
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
        VoucherType.RECEIPT -> "قبض" to GreenCredit
        VoucherType.PAYMENT -> "صرف" to CoralDebit
        VoucherType.INVOICE_CREDIT -> "آجل" to CoralDebit
    }
    val balance = voucher.runningBalance
    val balanceLabel = when {
        balance > 0 -> "عليه"
        balance < 0 -> "له"
        else -> "خالص"
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedgerCell(dateStr, 1.05f, false)
            LedgerCell(typeText, 0.62f, true, typeColor)
            LedgerCell(EscPos58mmPrinterService.formatAmount(voucher.amount), 0.95f, true,
                if (voucher.credit > 0) GreenCredit else CoralDebit)
            LedgerCell(voucher.statement.ifBlank { "—" }, 1.35f, false, maxLines = 2)
            LedgerCell("${EscPos58mmPrinterService.formatAmount(kotlin.math.abs(balance))} ${balanceLabel}", 1.05f, true,
                if (balance > 0) CoralDebit else if (balance < 0) GreenCredit else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun RowScope.LedgerCell(
    value: String,
    weight: Float,
    emphasize: Boolean,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = 1
) {
    Text(
        text = value,
        modifier = Modifier.weight(weight),
        fontSize = if (emphasize) 11.sp else 10.sp,
        color = color,
        fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
        textAlign = TextAlign.Center,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        lineHeight = 12.sp
    )
}

@Composable
private fun rememberDate(timestamp: Long): String {
    return androidx.compose.runtime.remember(timestamp) {
        SimpleDateFormat("MM/dd HH:mm", Locale.US).format(Date(timestamp))
    }
}
