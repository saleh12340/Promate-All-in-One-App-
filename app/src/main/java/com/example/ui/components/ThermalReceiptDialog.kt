package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.service.EscPos58mmPrinterService
import com.example.service.PdfReceiptExporter
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream

@Composable
fun ThermalReceiptDialog(
    bitmap: Bitmap?,
    invoice: Invoice?,
    items: List<InvoiceItem>,
    storeName: String,
    storePhone: String,
    onDismiss: () -> Unit
) {
    if (bitmap == null || invoice == null) return
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "معاينة الفاتورة الحرارية (58mm)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Thermal Paper Slip Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEBEBEB))
                        .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        shadowElevation = 4.dp,
                        color = Color.White
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "فاتورة حرارية 58mm",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Thermal Print
                    Button(
                        onClick = {
                            printReceiptBitmap(context, bitmap, invoice.invoiceNumber)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("طباعة 58mm", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Share Bitmap Image (WhatsApp)
                    OutlinedButton(
                        onClick = {
                            shareReceiptBitmap(context, bitmap, invoice.invoiceNumber)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة صورة", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Export PDF Button
                OutlinedButton(
                    onClick = {
                        try {
                            val pdf = PdfReceiptExporter.generateInvoicePdf(
                                context = context,
                                storeName = storeName,
                                storePhone = storePhone,
                                invoice = invoice,
                                items = items
                            )
                            sharePdfFile(context, pdf)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر إنشاء ملف PDF، حاول مرة أخرى", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصدير مستند PDF رسمي", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun shareReceiptBitmap(context: Context, bitmap: Bitmap, invoiceNum: String) {
    try {
        val file = File(context.cacheDir, "receipt_$invoiceNum.png")
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
    } catch (e: Exception) {
        Toast.makeText(context, "تم حفظ صورة الفاتورة في الذاكرة المؤقتة", Toast.LENGTH_SHORT).show()
    }
}

private fun sharePdfFile(context: Context, pdfFile: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة ملف PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "تم حفظ ملف PDF: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
    }
}

private fun printReceiptBitmap(context: Context, bitmap: Bitmap, invoiceNum: String) {
    try {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val adapter = object : PrintDocumentAdapter() {
            private var documentInfo: PrintDocumentInfo? = null

            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal.isCanceled) {
                    callback.onLayoutCancelled()
                    return
                }
                documentInfo = PrintDocumentInfo.Builder("invoice_$invoiceNum.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback.onLayoutFinished(documentInfo, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal,
                callback: WriteResultCallback
            ) {
                var pdf: PdfDocument? = null
                try {
                    if (cancellationSignal.isCanceled) {
                        callback.onWriteCancelled()
                        return
                    }
                    val pageWidth = 595
                    val pageHeight = (pageWidth * bitmap.height.toFloat() / bitmap.width).toInt().coerceAtLeast(200)
                    pdf = PdfDocument()
                    val page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
                    val target = RectF(12f, 12f, pageWidth - 12f, pageHeight - 12f)
                    page.canvas.drawColor(android.graphics.Color.WHITE)
                    page.canvas.drawBitmap(bitmap, null, target, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG))
                    pdf.finishPage(page)
                    FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                } finally {
                    pdf?.close()
                }
            }
        }
        printManager.print("فاتورة_$invoiceNum", adapter, PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
            .setResolution(android.print.PrintAttributes.Resolution("receipt", "Receipt", 203, 203))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build())
    } catch (e: Exception) {
        Toast.makeText(context, "تعذر فتح خدمة الطباعة: ${e.message ?: "خطأ غير معروف"}", Toast.LENGTH_LONG).show()
    }
}
