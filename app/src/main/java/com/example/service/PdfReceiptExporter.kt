package com.example.service

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReceiptExporter {

    fun generateInvoicePdf(
        context: Context,
        storeName: String,
        storePhone: String,
        invoice: Invoice,
        items: List<InvoiceItem>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (595 x 842 pts)
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(27, 67, 50) // Deep Emerald
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(235, 243, 238)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val textBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 30, 30)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(200, 210, 205)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        // Top Banner
        canvas.drawRect(36f, 36f, 559f, 96f, headerBgPaint)
        canvas.drawRect(36f, 36f, 559f, 96f, borderPaint)

        canvas.drawText(storeName, 50f, 66f, primaryPaint)
        if (storePhone.isNotBlank()) {
            canvas.drawText("هاتف: $storePhone", 50f, 84f, textPaint)
        }

        val typeBadge = "فاتورة مبيعات ${invoice.paymentType.labelAr}"
        canvas.drawText(typeBadge, 400f, 66f, titlePaint)
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        val dateStr = dateFormat.format(Date(invoice.createdAt))
        canvas.drawText("التاريخ: $dateStr", 400f, 84f, textPaint)

        // Metadata box
        var y = 120f
        canvas.drawText("رقم الفاتورة: ${invoice.invoiceNumber}", 50f, y, textBoldPaint)
        if (invoice.customerName.isNotBlank()) {
            canvas.drawText("العميل: ${invoice.customerName}", 260f, y, textBoldPaint)
        }
        y += 24f

        // Table Header
        canvas.drawRect(36f, y, 559f, y + 26f, headerBgPaint)
        canvas.drawRect(36f, y, 559f, y + 26f, borderPaint)

        canvas.drawText("م", 46f, y + 17f, textBoldPaint)
        canvas.drawText("بيان الصنف", 76f, y + 17f, textBoldPaint)
        canvas.drawText("الكمية", 320f, y + 17f, textBoldPaint)
        canvas.drawText("سعر الوحدة (ر.ي)", 380f, y + 17f, textBoldPaint)
        canvas.drawText("الإجمالي (ر.ي)", 480f, y + 17f, textBoldPaint)
        y += 26f

        // Table Rows
        items.forEachIndexed { index, item ->
            val rowHeight = 24f
            canvas.drawRect(36f, y, 559f, y + rowHeight, borderPaint)

            canvas.drawText("${index + 1}", 46f, y + 16f, textPaint)
            val desc = if (item.description.length > 36) item.description.take(33) + "..." else item.description
            canvas.drawText(desc, 76f, y + 16f, textPaint)

            val qtyStr = EscPos58mmPrinterService.formatQuantity(item.quantity)
            val priceStr = EscPos58mmPrinterService.formatAmount(item.unitPrice)
            val totalStr = EscPos58mmPrinterService.formatAmount(item.totalPrice)

            canvas.drawText(qtyStr, 320f, y + 16f, textPaint)
            canvas.drawText(priceStr, 380f, y + 16f, textPaint)
            canvas.drawText(totalStr, 480f, y + 16f, textPaint)

            y += rowHeight
        }

        y += 18f
        // Totals Card on right
        val boxLeft = 340f
        val boxRight = 559f
        val boxHeight = 70f
        canvas.drawRect(boxLeft, y, boxRight, y + boxHeight, headerBgPaint)
        canvas.drawRect(boxLeft, y, boxRight, y + boxHeight, borderPaint)

        canvas.drawText("الإجمالي الكلي:", boxLeft + 14f, y + 22f, textBoldPaint)
        canvas.drawText("${EscPos58mmPrinterService.formatAmount(invoice.grandTotal)} ر.ي", boxLeft + 120f, y + 22f, titlePaint)

        if (invoice.paidAmount > 0) {
            canvas.drawText("المدفوع:", boxLeft + 14f, y + 42f, textPaint)
            canvas.drawText("${EscPos58mmPrinterService.formatAmount(invoice.paidAmount)} ر.ي", boxLeft + 120f, y + 42f, textBoldPaint)
        }
        if (invoice.remainingAmount > 0) {
            canvas.drawText("المتبقي الآجل:", boxLeft + 14f, y + 60f, textPaint)
            canvas.drawText("${EscPos58mmPrinterService.formatAmount(invoice.remainingAmount)} ر.ي", boxLeft + 120f, y + 60f, textBoldPaint)
        }

        if (invoice.notes.isNotBlank()) {
            canvas.drawText("ملاحظات: ${invoice.notes}", 50f, y + 30f, textPaint)
        }

        // Footer
        canvas.drawText("تم إنشاء الفاتورة بواسطة تطبيق المحاسب الذكي - نظام محاسبي مستقل ومرن", 140f, 800f, textPaint)

        document.finishPage(page)

        val outputFile = File(context.cacheDir, "invoice_${invoice.invoiceNumber}.pdf")
        val outStream = FileOutputStream(outputFile)
        document.writeTo(outStream)
        outStream.close()
        document.close()

        return outputFile
    }
}
