package com.example.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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
        val pageWidth = 595
        val pageHeight = 842
        val right = 559f
        val left = 36f
        val normal = paint(11f)
        val bold = paint(11f, true)
        val small = paint(9f)
        val title = paint(19f, true, Color.rgb(24, 62, 91))
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 241, 248) }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 205, 216)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f

        fun drawRight(text: String, x: Float, baseline: Float, p: Paint) {
            p.textAlign = Paint.Align.RIGHT
            canvas?.drawText(text, x, baseline, p)
            p.textAlign = Paint.Align.LEFT
        }
        fun beginPage() {
            if (page != null) document.finishPage(page!!)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page!!.canvas
            canvas!!.drawColor(Color.WHITE)
            canvas!!.drawRoundRect(RectF(left, 28f, right, 102f), 8f, 8f, headerBg)
            canvas!!.drawRoundRect(RectF(left, 28f, right, 102f), 8f, 8f, border)
            drawRight(storeName.ifBlank { "فاتورة" }, right - 14f, 55f, title)
            if (storePhone.isNotBlank()) drawRight("هاتف: $storePhone", right - 14f, 76f, normal)
            drawRight("فاتورة مبيعات • ${invoice.paymentType.labelAr}", left + 14f, 55f, bold)
            val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(invoice.createdAt))
            drawRight("التاريخ: $date", left + 14f, 76f, normal)
            y = 126f
            drawRight("رقم الفاتورة: ${invoice.invoiceNumber}", right, y, bold)
            if (invoice.customerName.isNotBlank()) {
                drawRight("العميل: ${invoice.customerName}", 350f, y, normal)
            }
            y += 25f
            canvas!!.drawRect(left, y - 17f, right, y + 8f, headerBg)
            canvas!!.drawRect(left, y - 17f, right, y + 8f, border)
            drawRight("الإجمالي", 96f, y, bold)
            drawRight("سعر الوحدة", 190f, y, bold)
            drawRight("الكمية", 265f, y, bold)
            drawRight("بيان الصنف", 500f, y, bold)
            drawRight("م", 545f, y, bold)
            y += 10f
        }
        fun ensureSpace(height: Float) {
            if (y + height > 770f) beginPage()
        }
        fun wrapText(text: String, p: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return listOf("—")
            val result = mutableListOf<String>()
            var remaining = text.trim()
            while (remaining.isNotEmpty()) {
                val count = p.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                var split = count.coerceAtMost(remaining.length)
                if (split < remaining.length) {
                    val space = remaining.lastIndexOf(' ', split - 1)
                    if (space > 0) split = space
                }
                result += remaining.substring(0, split).trim()
                remaining = remaining.substring(split).trim()
            }
            return result
        }

        beginPage()
        items.forEachIndexed { index, item ->
            val descLines = wrapText(item.description, normal, 220f)
            val rowHeight = (descLines.size * 15f + 8f).coerceAtLeast(27f)
            ensureSpace(rowHeight)
            canvas!!.drawLine(left, y - 8f, right, y - 8f, border)
            val baseline = y + 8f
            drawRight(EscPos58mmPrinterService.formatAmount(item.totalPrice), 96f, baseline, bold)
            drawRight(EscPos58mmPrinterService.formatAmount(item.unitPrice), 190f, baseline, normal)
            drawRight(EscPos58mmPrinterService.formatQuantity(item.quantity), 265f, baseline, normal)
            drawRight("${index + 1}", 545f, baseline, small)
            descLines.forEachIndexed { lineIndex, line ->
                drawRight(line, 500f, baseline + lineIndex * 15f, normal)
            }
            y += rowHeight
        }

        val totalsHeight = 112f + if (invoice.notes.isNotBlank()) 30f else 0f
        ensureSpace(totalsHeight)
        y += 12f
        canvas!!.drawRoundRect(RectF(300f, y, right, y + 94f), 8f, 8f, headerBg)
        canvas!!.drawRoundRect(RectF(300f, y, right, y + 94f), 8f, 8f, border)
        drawRight("الإجمالي الكلي: ${EscPos58mmPrinterService.formatAmount(invoice.grandTotal)} ر.ي", right - 12f, y + 22f, bold)
        drawRight("المدفوع: ${EscPos58mmPrinterService.formatAmount(invoice.paidAmount)} ر.ي", right - 12f, y + 45f, normal)
        drawRight("المتبقي: ${EscPos58mmPrinterService.formatAmount(invoice.remainingAmount)} ر.ي", right - 12f, y + 68f, bold)
        y += 108f
        if (invoice.notes.isNotBlank()) {
            wrapText("ملاحظات: ${invoice.notes}", normal, 500f).forEach { line ->
                ensureSpace(18f)
                drawRight(line, right, y, normal)
                y += 17f
            }
        }
        ensureSpace(32f)
        drawRight("شكراً لتعاملكم معنا", right, y + 10f, bold)
        drawRight("صفحة $pageNumber", left + 5f, y + 10f, small)

        if (page != null) document.finishPage(page!!)
        val outputFile = File(context.cacheDir, "invoice_${invoice.invoiceNumber}.pdf")
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
        return outputFile
    }

    private fun paint(size: Float, isBold: Boolean = false, color: Int = Color.rgb(32, 38, 43)) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        }
}
