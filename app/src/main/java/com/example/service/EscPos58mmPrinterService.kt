package com.example.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EscPos58mmPrinterService {
    const val LINE_CHARS = 32 // Standard 58mm thermal printer line width

    // ESC/POS Command Constants
    private val ESC_INIT = byteArrayOf(0x1B, 0x40)
    private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01)
    private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)
    private val ESC_ALIGN_RIGHT = byteArrayOf(0x1B, 0x61, 0x02)
    private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)
    private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)
    private val ESC_FEED_PAPER = byteArrayOf(0x1B, 0x64, 0x03)

    fun getPairedPrinters(context: Context): List<BluetoothDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                adapter.bondedDevices.toList()
            } else {
                emptyList()
            }
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Builds raw ESC/POS byte stream for 58mm portable thermal printer
     */
    fun buildEscPosBytes(
        storeName: String,
        storePhone: String,
        invoice: Invoice,
        items: List<InvoiceItem>
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        val dateStr = dateFormat.format(Date(invoice.createdAt))

        out.write(ESC_INIT)
        out.write(ESC_ALIGN_CENTER)
        out.write(ESC_BOLD_ON)
        out.write("$storeName\n".toByteArray(Charsets.UTF_8))
        out.write(ESC_BOLD_OFF)
        if (storePhone.isNotBlank()) {
            out.write("هاتف: $storePhone\n".toByteArray(Charsets.UTF_8))
        }
        out.write("فاتورة مبيعات ${invoice.paymentType.labelAr}\n".toByteArray(Charsets.UTF_8))
        out.write(dashedLine().toByteArray(Charsets.UTF_8))

        out.write(ESC_ALIGN_RIGHT)
        out.write("رقم الفاتورة: ${invoice.invoiceNumber}\n".toByteArray(Charsets.UTF_8))
        out.write("التاريخ: $dateStr\n".toByteArray(Charsets.UTF_8))
        if (invoice.customerName.isNotBlank()) {
            out.write("العميل: ${invoice.customerName}\n".toByteArray(Charsets.UTF_8))
        }
        out.write(dashedLine().toByteArray(Charsets.UTF_8))

        // Table Header (32 chars)
        out.write(ESC_BOLD_ON)
        out.write(formatColumns("الصنف", "الكمية", "السعر", "الإجمالي").toByteArray(Charsets.UTF_8))
        out.write(ESC_BOLD_OFF)
        out.write(dashedLine().toByteArray(Charsets.UTF_8))

        // Items
        for (item in items) {
            val qtyStr = formatQuantity(item.quantity)
            val priceStr = formatAmount(item.unitPrice)
            val totalStr = formatAmount(item.totalPrice)
            out.write("${item.description}\n".toByteArray(Charsets.UTF_8))
            out.write(formatColumns("", qtyStr, priceStr, totalStr).toByteArray(Charsets.UTF_8))
        }
        out.write(dashedLine().toByteArray(Charsets.UTF_8))

        // Totals
        out.write(ESC_BOLD_ON)
        out.write(formatSummaryRow("الإجمالي:", "${formatAmount(invoice.grandTotal)} ر.ي").toByteArray(Charsets.UTF_8))
        if (invoice.paidAmount > 0) {
            out.write(formatSummaryRow("المدفوع:", "${formatAmount(invoice.paidAmount)} ر.ي").toByteArray(Charsets.UTF_8))
        }
        if (invoice.remainingAmount > 0) {
            out.write(formatSummaryRow("المتبقي:", "${formatAmount(invoice.remainingAmount)} ر.ي").toByteArray(Charsets.UTF_8))
        }
        out.write(ESC_BOLD_OFF)

        out.write(dashedLine().toByteArray(Charsets.UTF_8))
        out.write(ESC_ALIGN_CENTER)
        out.write("شكراً لتعاملكم معنا\n".toByteArray(Charsets.UTF_8))
        out.write(ESC_FEED_PAPER)

        return out.toByteArray()
    }

    /**
     * Renders a high-resolution 58mm thermal receipt Bitmap for live preview and image sharing
     */
    fun renderReceiptBitmap(
        storeName: String,
        storePhone: String,
        invoice: Invoice,
        items: List<InvoiceItem>
    ): Bitmap {
        val width = 384 // 58mm standard width at 203 DPI (dots per inch)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            strokeWidth = 2f
        }

        // Measure estimated height
        val baseHeight = 360 + (items.size * 54) + 160
        val height = baseHeight.coerceAtLeast(400)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        var y = 40f
        fun drawCenterText(text: String, paint: Paint) {
            val textWidth = paint.measureText(text)
            val x = (width - textWidth) / 2f
            canvas.drawText(text, x, y, paint)
            y += paint.textSize + 12f
        }

        fun drawDivider() {
            y += 4f
            canvas.drawLine(16f, y, width - 16f, y, linePaint)
            y += 18f
        }

        // Header
        drawCenterText(storeName, titlePaint)
        if (storePhone.isNotBlank()) {
            drawCenterText("هاتف: $storePhone", textPaint)
        }
        drawCenterText("فاتورة مبيعات (${invoice.paymentType.labelAr})", boldPaint)
        drawDivider()

        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
        val dateStr = dateFormat.format(Date(invoice.createdAt))

        // Info
        canvas.drawText("رقم الفاتورة: ${invoice.invoiceNumber}", 16f, y, textPaint)
        y += 28f
        canvas.drawText("التاريخ: $dateStr", 16f, y, textPaint)
        y += 28f
        if (invoice.customerName.isNotBlank()) {
            canvas.drawText("العميل: ${invoice.customerName}", 16f, y, textPaint)
            y += 28f
        }
        drawDivider()

        // Table Header
        canvas.drawText("الصنف", 16f, y, boldPaint)
        canvas.drawText("الكمية", 170f, y, boldPaint)
        canvas.drawText("السعر", 235f, y, boldPaint)
        val totalLabelWidth = boldPaint.measureText("الإجمالي")
        canvas.drawText("الإجمالي", width - 16f - totalLabelWidth, y, boldPaint)
        y += 28f
        drawDivider()

        // Items
        for (item in items) {
            canvas.drawText(item.description, 16f, y, boldPaint)
            y += 24f
            val qStr = formatQuantity(item.quantity)
            val pStr = formatAmount(item.unitPrice)
            val tStr = formatAmount(item.totalPrice)
            canvas.drawText(qStr, 170f, y, textPaint)
            canvas.drawText(pStr, 235f, y, textPaint)
            val itemTotalWidth = textPaint.measureText(tStr)
            canvas.drawText(tStr, width - 16f - itemTotalWidth, y, textPaint)
            y += 30f
        }
        drawDivider()

        // Totals
        fun drawTotalLine(label: String, amount: Double) {
            canvas.drawText(label, 16f, y, boldPaint)
            val valStr = "${formatAmount(amount)} ر.ي"
            val valWidth = boldPaint.measureText(valStr)
            canvas.drawText(valStr, width - 16f - valWidth, y, boldPaint)
            y += 30f
        }

        drawTotalLine("الإجمالي الكلي:", invoice.grandTotal)
        if (invoice.paidAmount > 0) {
            drawTotalLine("المدفوع:", invoice.paidAmount)
        }
        if (invoice.remainingAmount > 0) {
            drawTotalLine("المتبقي الآجل:", invoice.remainingAmount)
        }

        drawDivider()
        drawCenterText("شكراً لتعاملكم معنا", boldPaint)
        drawCenterText("*** المحاسب الذكي ***", textPaint)

        return bitmap
    }

    fun dashedLine(): String = "--------------------------------\n"

    fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            String.format(Locale.US, "%.0f", qty)
        } else {
            String.format(Locale.US, "%.2f", qty)
        }
    }

    fun formatAmount(amount: Double): String {
        return String.format(Locale.US, "%,.0f", amount)
    }

    private fun formatColumns(c1: String, c2: String, c3: String, c4: String): String {
        return String.format(Locale.US, "%-12s %4s %7s %7s\n", c1.take(12), c2.take(4), c3.take(7), c4.take(7))
    }

    private fun formatSummaryRow(label: String, value: String): String {
        val spaces = (LINE_CHARS - label.length - value.length).coerceAtLeast(1)
        return label + " ".repeat(spaces) + value + "\n"
    }
}
