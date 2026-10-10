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
        val width = 384
        val margin = 18f
        val contentWidth = width - margin * 2
        fun makePaint(size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = size
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }
        val body = makePaint(20f)
        val bold = makePaint(21f, true)
        val title = makePaint(27f, true)
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 1.5f }
        fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return listOf("—")
            val lines = mutableListOf<String>()
            var rest = text.trim()
            while (rest.isNotEmpty()) {
                val count = paint.breakText(rest, true, maxWidth, null).coerceAtLeast(1)
                var cut = count.coerceAtMost(rest.length)
                if (cut < rest.length) {
                    val space = rest.lastIndexOf(' ', cut - 1)
                    if (space > 0) cut = space
                }
                lines.add(rest.substring(0, cut).trim())
                rest = rest.substring(cut).trim()
            }
            return lines
        }
        val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(invoice.createdAt))
        val height = 260 + items.sumOf { wrap(it.description, bold, contentWidth.toFloat()).size * 25 + 38 } +
            (if (storePhone.isNotBlank()) 28 else 0) +
            (if (invoice.paidAmount > 0) 28 else 0) +
            (if (invoice.remainingAmount > 0) 28 else 0)
        val bitmap = Bitmap.createBitmap(width, height.coerceAtLeast(420), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = 36f
        val right = width - margin
        fun drawRight(text: String, paint: Paint, gap: Float = paint.textSize + 8f) {
            canvas.drawText(text, right, y, paint)
            y += gap
        }
        fun drawCenter(text: String, paint: Paint, gap: Float = paint.textSize + 8f) {
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(text, width / 2f, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            y += gap
        }
        fun line() {
            y += 4f
            canvas.drawLine(margin, y, width - margin, y, divider)
            y += 19f
        }

        drawCenter(storeName.ifBlank { "فاتورة" }, title, 36f)
        if (storePhone.isNotBlank()) drawCenter("هاتف: $storePhone", body)
        drawCenter("فاتورة مبيعات • ${invoice.paymentType.labelAr}", bold)
        line()
        drawRight("رقم الفاتورة: ${invoice.invoiceNumber}", body)
        drawRight("التاريخ: $dateStr", body)
        if (invoice.customerName.isNotBlank()) {
            wrap("العميل: ${invoice.customerName}", body, contentWidth).forEach { drawRight(it, body, 27f) }
        }
        line()
        drawRight("الأصناف", bold, 27f)
        line()
        items.forEach { item ->
            wrap(item.description, bold, contentWidth).forEach { drawRight(it, bold, 25f) }
            val numeric = "الكمية: ${formatQuantity(item.quantity)}    السعر: ${formatAmount(item.unitPrice)}"
            wrap(numeric, body, contentWidth).forEach { drawRight(it, body, 25f) }
            drawRight("الإجمالي: ${formatAmount(item.totalPrice)} ر.ي", bold, 29f)
            line()
        }
        drawRight("الإجمالي الكلي: ${formatAmount(invoice.grandTotal)} ر.ي", title, 35f)
        if (invoice.paidAmount > 0) drawRight("المدفوع: ${formatAmount(invoice.paidAmount)} ر.ي", body, 29f)
        if (invoice.remainingAmount > 0) drawRight("المتبقي: ${formatAmount(invoice.remainingAmount)} ر.ي", bold, 30f)
        line()
        drawCenter("شكراً لتعاملكم معنا", bold)
        return Bitmap.createBitmap(bitmap, 0, 0, width, y.toInt().coerceAtMost(bitmap.height).coerceAtLeast(1))
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
