package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.PaymentType
import com.example.service.EscPos58mmPrinterService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertTrue(appName.isNotEmpty())
  }

  @Test
  fun `verify clean quantity formatting`() {
    assertEquals("1", EscPos58mmPrinterService.formatQuantity(1.0))
    assertEquals("5", EscPos58mmPrinterService.formatQuantity(5.0))
    assertEquals("2.50", EscPos58mmPrinterService.formatQuantity(2.5))
    assertEquals("0.75", EscPos58mmPrinterService.formatQuantity(0.75))
  }

  @Test
  fun `verify 58mm esc pos bytes generation`() {
    val invoice = Invoice(
      invoiceNumber = "INV-0001",
      customerName = "مؤسسة الأمل",
      paymentType = PaymentType.CASH,
      grandTotal = 50000.0,
      paidAmount = 50000.0
    )
    val items = listOf(
      InvoiceItem(description = "سكر برازيلي", quantity = 2.0, unitPrice = 25000.0, totalPrice = 50000.0)
    )
    val bytes = EscPos58mmPrinterService.buildEscPosBytes(
      storeName = "مؤسسة الرواد",
      storePhone = "777000111",
      invoice = invoice,
      items = items
    )
    assertTrue(bytes.isNotEmpty())
  }
}
