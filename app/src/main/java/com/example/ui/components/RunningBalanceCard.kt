package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    val dateFormat = SimpleDateFormat("yyyy/MM/dd  hh:mm a", Locale.US)
    val dateStr = dateFormat.format(Date(voucher.createdAt))

    val (badgeText, badgeColor, badgeIcon) = when (voucher.voucherType) {
        VoucherType.RECEIPT -> Triple("سند قبض", GreenCredit, Icons.Default.ArrowDownward)
        VoucherType.PAYMENT -> Triple("سند صرف", CoralDebit, Icons.Default.ArrowUpward)
        VoucherType.INVOICE_CREDIT -> Triple("فاتورة آجلة", CoralDebit, Icons.Default.Receipt)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Type Badge + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Text(
                    text = dateStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Middle: Statement
            Text(
                text = voucher.statement,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Grid: Debit / Credit / Running Balance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Movement Amount
                Column {
                    val isCredit = voucher.credit > 0
                    Text(
                        text = if (isCredit) "دائن (واصل)" else "مدين (عليه)",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${EscPos58mmPrinterService.formatAmount(voucher.amount)} ر.ي",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCredit) GreenCredit else CoralDebit
                    )
                }

                // Running Balance
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "الرصيد التراكمي المستمر",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val balance = voucher.runningBalance
                    val balanceStatus = if (balance > 0) "له بذمته" else if (balance < 0) "رصيد دائن" else "خالص"
                    Text(
                        text = "${EscPos58mmPrinterService.formatAmount(kotlin.math.abs(balance))} ر.ي ($balanceStatus)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balance > 0) CoralDebit else if (balance < 0) GreenCredit else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
