package com.example.ui.screens

import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import android.os.Process
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.ui.theme.CellBorderColor
import com.example.ui.theme.DarkText
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.AccountingViewModel

@Composable
fun StoreProfileScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentName by viewModel.storeName.collectAsState()
    val currentPhone by viewModel.storePhone.collectAsState()

    var name by remember(currentName) { mutableStateOf(currentName) }
    var phone by remember(currentPhone) { mutableStateOf(currentPhone) }
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null) try {
            val dbFile = context.getDatabasePath("smart_invoicing_db")
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { db -> db.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() } }
            context.contentResolver.openOutputStream(uri, "wt")!!.use { out -> dbFile.inputStream().use { it.copyTo(out) } }
            Toast.makeText(context, "تم إنشاء النسخة الاحتياطية", Toast.LENGTH_LONG).show()
        } catch (e: Exception) { Toast.makeText(context, "تعذر إنشاء النسخة الاحتياطية", Toast.LENGTH_LONG).show() }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) try {
            val temp = java.io.File(context.cacheDir, "restore_database.tmp")
            context.contentResolver.openInputStream(uri)!!.use { input -> temp.outputStream().use { input.copyTo(it) } }
            val check = SQLiteDatabase.openDatabase(temp.path, null, SQLiteDatabase.OPEN_READONLY)
            val valid = check.rawQuery("PRAGMA integrity_check", null).use { it.moveToFirst() && it.getString(0) == "ok" }; check.close()
            if (!valid) throw IllegalArgumentException("ملف النسخة غير صالح")
            AppDatabase.closeDatabase()
            val target = context.getDatabasePath("smart_invoicing_db")
            java.io.File(target.path + "-wal").delete(); java.io.File(target.path + "-shm").delete()
            temp.copyTo(target, overwrite = true)
            Toast.makeText(context, "تمت الاستعادة، سيعاد تشغيل التطبيق", Toast.LENGTH_LONG).show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 900)
        } catch (e: Exception) { Toast.makeText(context, "فشلت الاستعادة: تحقق من ملف النسخة", Toast.LENGTH_LONG).show() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 10.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Store,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "بيانات المنشأة والمتجر",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المتجر") },
                    placeholder = { Text("مثال: بقالة العزي") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CellBorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFFAFCFF),
                        cursorColor = PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف") },
                    placeholder = { Text("7xxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = CellBorderColor
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        viewModel.storeName.value = name.trim().ifBlank { "بقالة العزي" }
                        viewModel.storePhone.value = phone.trim()
                        Toast.makeText(context, "تم حفظ بيانات المتجر بنجاح", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ التغييرات", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("النسخ الاحتياطي والاستعادة", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = DarkText)
                Spacer(Modifier.height(6.dp))
                Text("احفظ بيانات العملاء والفواتير والحركات في ملف لاستعادتها عند الحاجة.", fontSize = 13.sp, color = DarkText)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { backupLauncher.launch("smart-invoicing-backup.db") }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("إنشاء نسخة احتياطية", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { showRestoreConfirmation = true }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB42318))) { Text("استعادة نسخة احتياطية", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        if (showRestoreConfirmation) {
            AlertDialog(onDismissRequest = { showRestoreConfirmation = false }, title = { Text("تأكيد الاستعادة") }, text = { Text("ستُستبدل البيانات الحالية ببيانات النسخة الاحتياطية. أنشئ نسخة من بياناتك الحالية أولاً. هل تريد المتابعة؟") }, confirmButton = { TextButton(onClick = { showRestoreConfirmation = false; restoreLauncher.launch(arrayOf("*/*")) }) { Text("متابعة الاستعادة") } }, dismissButton = { TextButton(onClick = { showRestoreConfirmation = false }) { Text("إلغاء") } }, shape = RoundedCornerShape(20.dp))
        }
        Spacer(Modifier.height(16.dp))
        // Thermal Printing Specs card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعدادات الطباعة الحرارية (58mm)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• قياس الورق: طابعات فواتير حرارية 58mm القياسية المحمولة (32 حرفاً بالسطر).\n• بروتوكول الطباعة: أوامر ESC/POS المباشرة وبث البت ماب (Bitmap) عبر البلوتوث.\n• العملة المعتمدة: الريال اليمني (ر.ي / YER).",
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    color = DarkText
                )
            }
        }
    }
}
