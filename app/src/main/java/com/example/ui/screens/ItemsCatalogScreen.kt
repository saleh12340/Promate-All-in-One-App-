package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.EscPos58mmPrinterService
import com.example.ui.theme.CellBorderColor
import com.example.ui.theme.DarkText
import com.example.ui.theme.LightBluePill
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.TextBlue
import com.example.ui.viewmodel.AccountingViewModel

data class CatalogItem(
    val name: String,
    val defaultPrice: Double,
    val category: String
)

val defaultCatalogItems = listOf(
    CatalogItem("كيس بر أسترالي 50كجم", 13500.0, "حبوب ودقيق"),
    CatalogItem("نص سكر برازيلي", 10900.0, "مواد غذائية"),
    CatalogItem("دبه زيت نباتي 20 لتر", 4800.0, "زيوت"),
    CatalogItem("قطمه رز بسمتي هندي", 4000.0, "أرز"),
    CatalogItem("قطمه رز عايدي درجة أولى", 3800.0, "أرز"),
    CatalogItem("سمن الماس بلدي", 1700.0, "معلبات وسمن"),
    CatalogItem("حوايج وهرد ممتاز", 1400.0, "بهارات"),
    CatalogItem("نص صلصه طماطم ممتار", 1300.0, "صلصات"),
    CatalogItem("كفف و2كوافي", 1200.0, "مستلزمات"),
    CatalogItem("شحم طبي كبير", 1000.0, "متفرقات"),
    CatalogItem("قشر يمني بلدي", 1000.0, "بن وقشر"),
    CatalogItem("خميره كبير باكيت", 900.0, "مخبوزات"),
    CatalogItem("شاهي ربع فرط أحمر", 900.0, "شاي ومنبهات"),
    CatalogItem("احذي حمام حمراء", 800.0, "بلاستيكيات"),
    CatalogItem("مقوش عاج أبيض", 500.0, "كماليات"),
    CatalogItem("قطمه صابون غسيل", 4700.0, "منظفات"),
    CatalogItem("حلبه بلدي مطحونة", 300.0, "بهارات")
)

@Composable
fun ItemsCatalogScreen(
    viewModel: AccountingViewModel,
    onItemSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = defaultCatalogItems.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 10.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث في دليل الأصناف...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryBlue)
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredItems) { item ->
                Card(
                    onClick = {
                        viewModel.topItemDetails.value = item.name
                        viewModel.topItemUnitPrice.value = String.format(java.util.Locale.US, "%.0f", item.defaultPrice)
                        viewModel.topItemQty.value = "1"
                        viewModel.topItemTotal.value = EscPos58mmPrinterService.formatAmount(item.defaultPrice)
                        onItemSelected()
                        Toast.makeText(context, "تم اختيار: ${item.name}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkText)
                            Text(text = item.category, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${EscPos58mmPrinterService.formatAmount(item.defaultPrice)} ر.ي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = LightBluePill
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "إضافة للفاتورة",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.padding(4.dp).size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
