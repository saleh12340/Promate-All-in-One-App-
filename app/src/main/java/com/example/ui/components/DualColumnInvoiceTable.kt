package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.EscPos58mmPrinterService
import com.example.ui.theme.CellBorderColor
import com.example.ui.theme.DarkText
import com.example.ui.theme.NavyHeader
import com.example.ui.theme.TextBlue
import com.example.ui.viewmodel.InvoiceItemDraft

@Composable
fun DualColumnInvoiceTable(
    rows: List<InvoiceItemDraft>,
    onRemoveRow: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }

    // Pair items: item 0 on right, item 1 on left, item 2 on right, item 3 on left...
    val rowCount = ((rows.size + 1) / 2).coerceAtLeast(4)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CellBorderColor, RoundedCornerShape(12.dp))
            .testTag("dual_column_invoice_table"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Dark Navy Dual Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyHeader)
                    .padding(vertical = 6.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left half columns: سعر الواحدة | التفاصيل | العدد | الإجمالي
                HeaderSection(modifier = Modifier.weight(1f))

                // Vertical Divider between left and right headers
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color.White.copy(alpha = 0.5f))
                )

                // Right half columns: سعر الواحدة | التفاصيل | العدد | الإجمالي
                HeaderSection(modifier = Modifier.weight(1f))
            }

            // Data Rows (Pairwise)
            for (i in 0 until rowCount) {
                val rightIndex = i * 2
                val leftIndex = i * 2 + 1

                val rightItem = rows.getOrNull(rightIndex)
                val leftItem = rows.getOrNull(leftIndex)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, CellBorderColor),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Item Half
                    ItemCellQuad(
                        item = leftItem,
                        onClick = {
                            if (leftIndex < rows.size) selectedItemIndex = leftIndex
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Vertical Center Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(34.dp)
                            .background(CellBorderColor)
                    )

                    // Right Item Half
                    ItemCellQuad(
                        item = rightItem,
                        onClick = {
                            if (rightIndex < rows.size) selectedItemIndex = rightIndex
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Delete item dialog when tapping a row
    if (selectedItemIndex != null) {
        val idx = selectedItemIndex!!
        val item = rows.getOrNull(idx)
        if (item != null) {
            AlertDialog(
                onDismissRequest = { selectedItemIndex = null },
                title = { Text("إدارة الصنف") },
                text = { Text("هل تريد حذف الصنف \"${item.description}\" بقيمة ${EscPos58mmPrinterService.formatAmount(item.totalPrice)} ر.ي؟") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onRemoveRow(idx)
                            selectedItemIndex = null
                        }
                    ) {
                        Text("حذف الصنف", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedItemIndex = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
private fun HeaderSection(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "سعر\nالواحدة",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = "التفاصيل",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.8f)
        )
        Text(
            text = "العدد",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.7f)
        )
        Text(
            text = "الإجمالي",
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.2f)
        )
    }
}

@Composable
private fun ItemCellQuad(
    item: InvoiceItemDraft?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(enabled = item != null && item.description.isNotBlank()) { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item != null && (item.description.isNotBlank() || item.unitPrice > 0)) {
            // سعر الواحدة (Blue)
            Text(
                text = EscPos58mmPrinterService.formatAmount(item.unitPrice),
                color = TextBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.1f)
            )

            // التفاصيل (Dark Text with Wrapping)
            Text(
                text = item.description,
                color = DarkText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.8f)
            )

            // العدد
            Text(
                text = item.quantityStr.ifBlank { "1" },
                color = DarkText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.7f)
            )

            // الإجمالي (Blue)
            Text(
                text = EscPos58mmPrinterService.formatAmount(item.totalPrice),
                color = TextBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.2f)
            )
        } else {
            // Empty placeholder row with 1 in Qty
            Text(text = "", modifier = Modifier.weight(1.1f))
            Text(text = "", modifier = Modifier.weight(1.8f))
            Text(
                text = "1",
                color = Color.LightGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.7f)
            )
            Text(text = "", modifier = Modifier.weight(1.2f))
        }
    }
}
