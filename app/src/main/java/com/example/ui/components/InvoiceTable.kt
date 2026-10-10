package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.EscPos58mmPrinterService
import com.example.ui.viewmodel.InvoiceItemDraft

@Composable
fun InvoiceTable(
    rows: List<InvoiceItemDraft>,
    onUpdateRow: (index: Int, description: String?, quantityStr: String?, unitPriceStr: String?) -> Unit,
    onRemoveRow: (index: Int) -> Unit,
    onVoiceClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("invoice_table_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // RTL header order: total, quantity, item details, unit price, row number.
            Row(
                modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الإجمالي", modifier = Modifier.weight(1.3f), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center, maxLines = 1)
                Text("الكمية", modifier = Modifier.weight(0.8f), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center, maxLines = 1)
                Text("التفاصيل", modifier = Modifier.weight(2.0f), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center, maxLines = 1)
                Text("سعر الواحدة", modifier = Modifier.weight(1.2f), fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center, maxLines = 1)
                Text("م", modifier = Modifier.width(24.dp), fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(28.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Rows
            rows.forEachIndexed { index, rowItem ->
                InvoiceTableRowItem(
                    index = index,
                    item = rowItem,
                    canDelete = rows.size > 2,
                    onUpdateDescription = { onUpdateRow(index, it, null, null) },
                    onUpdateQuantity = { onUpdateRow(index, null, it, null) },
                    onUpdateUnitPrice = { onUpdateRow(index, null, null, it) },
                    onDelete = { onRemoveRow(index) },
                    onVoiceClick = { onVoiceClick(index) }
                )
                if (index < rows.size - 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun InvoiceTableRowItem(
    index: Int,
    item: InvoiceItemDraft,
    canDelete: Boolean,
    onUpdateDescription: (String) -> Unit,
    onUpdateQuantity: (String) -> Unit,
    onUpdateUnitPrice: (String) -> Unit,
    onDelete: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // RTL order: total → quantity → details → unit price → row number → delete.
        val totalFormatted = EscPos58mmPrinterService.formatAmount(item.totalPrice)
        Text(
            text = totalFormatted,
            modifier = Modifier.weight(1.3f).padding(horizontal = 2.dp),
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center,
            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip
        )
        OutlinedTextField(
            value = item.quantityStr,
            onValueChange = onUpdateQuantity,
            modifier = Modifier.weight(0.8f).testTag("item_quantity_$index"),
            placeholder = { Text("1", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp),
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        OutlinedTextField(
            value = item.description,
            onValueChange = onUpdateDescription,
            modifier = Modifier.weight(2.0f).testTag("item_description_$index"),
            placeholder = { Text("اسم الصنف", fontSize = 10.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp),
            trailingIcon = {
                IconButton(onClick = onVoiceClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = "إدخال صوتي",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            },
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        OutlinedTextField(
            value = item.unitPriceStr,
            onValueChange = onUpdateUnitPrice,
            modifier = Modifier.weight(1.2f).testTag("item_price_$index"),
            placeholder = { Text("0", fontSize = 10.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontSize = 12.sp),
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        Text(
            text = "${index + 1}", modifier = Modifier.width(24.dp),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        if (canDelete) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp).testTag("item_delete_$index")) {
                Icon(Icons.Default.Close, contentDescription = "حذف السطر",
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            }
        } else {
            Spacer(modifier = Modifier.width(28.dp))
        }
    }
}
