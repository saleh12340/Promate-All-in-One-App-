# Smart Invoicing & Accounting Enterprise System (نظام الفواتير والمحاسبة الذكي)

An offline-first Android enterprise accounting and invoicing application built with Kotlin, Jetpack Compose, Material Design 3, Room SQLite database, high-resolution PDF and 58mm ESC/POS thermal receipt generation, customer running balance ledgers, and Gemini audio transcription for voice-driven item and note entry.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and configuration choices have been confirmed based on user feedback:
> - **Input Modality**: Dual text and voice input enabled across invoice item entry, customer notes, and voucher statements using Gemini audio transcription (`gemini-3.5-transcribe`).
> - **Primary Currency**: Yemeni Rial (`YER` / `ر.ي`) with standard Arabic/English digits (`0-9`), clean quantity formatting (e.g., `1`, `5` instead of `1.0`), and international dates.
> - **Thermal Printing**: Formatted specifically for compact 58mm mobile Bluetooth thermal printers (32 columns per line), alongside high-resolution bitmap receipt export and official tabular PDF generation.

---

### 1. Overview & Core Concept

- **What It Does**: Automates daily sales, invoice creation, customer ledger balance tracking, and voucher accounting (سندات القبض والصرف) with zero cloud dependency. Allows merchants to issue invoices with auto-expanding table rows, print directly to 58mm portable thermal printers, export cleanly formatted PDFs/images, and dictate item descriptions and notes using voice recording transcribed by Gemini.
- **Target Audience / Persona**: Small and medium commercial enterprises, wholesalers, retail shops, field sales representatives, and independent service providers who require fast, reliable accounting without internet reliance.
- **Key Value**: Eliminates data entry latency, provides instantaneous running balances on customer ledgers, prevents calculation errors, and delivers professional printable outputs directly from an Android handheld device.

---

### 2. User Experience & Visual Design

#### Key User Flows

1. **Interactive Invoicing Engine Flow**:
   - The user opens the New Invoice screen; the table begins with 2 empty item rows.
   - When the user types or dictates an item name, quantity, and unit price in row 1 and moves to row 2, the table automatically expands with a 3rd row without pressing extra buttons.
   - The user toggles between Cash (نقدي) and Credit (آجل). If Credit is selected, the invoice total instantly posts to the customer's account balance.
   - One-tap actions: Save, Print to 58mm Thermal, Share Receipt Image (Bitmap for WhatsApp), or Export PDF.

2. **Customer Ledger & Single-Row Action Bar Flow**:
   - Merchant views the customer profile showing aggregate balance (مدين / كفيل / دائن).
   - A single-row compact action bar at the top allows selecting Voucher Type (قبض Receipt / صرف Payment), entering Amount and Note (or mic button for voice memo), and tapping "إضافة سند" in one tap.
   - The cumulative card ledger underneath displays chronological entries with exact timestamps, transaction badges, statement details, and the continuous **Running Balance (الرصيد التراكمي)** after every single movement.

3. **Audio Transcription & Voice Dictation Flow**:
   - A microphone icon on item description fields and voucher note fields activates in-app recording with live decibel waveform animation.
   - Recorded audio is sent to Gemini (`gemini-3.5-transcribe` / REST audio endpoint) which transcribes Arabic dialect and commercial product names accurately into the text field.

4. **58mm Receipt & PDF Output Flow**:
   - Live preview modal showing the exact 58mm monochromatic thermal slip.
   - Actions to trigger Android Bluetooth printer pairing/ESC-POS byte generation, save high-res JPEG/PNG, or generate standard A4/A5 PDF.

#### Visual Identity & Theme

- **Aesthetic Direction**: Executive Modern Material 3 with soft neumorphic surface elevation, crisp borders, and dark emerald accent representing financial stability.
- **Color Palette**:
  - Primary / Brand: Deep Emerald Slate (`#1B4332` / `#2D6A4F`)
  - Accent / Credit: Vivid Teal (`#40916C` / `#52B788`)
  - Debit / Expense Warning: Coral Rust (`#BC4749`)
  - Neutral Background: Soft Cloud White (`#F8F9FA`) in light mode; Deep Obsidian (`#121816`) in dark mode
  - Surface Containers: Elevated card surfaces with soft ambient drop shadows and 12dp rounded corners (`#FFFFFF` / `#1C2321`)
- **Typography & Hierarchy**:
  - Numbers: High-contrast tabular figures using international digits `0-9` for perfect alignment.
  - Text: Clean Arabic typography with dynamic text wrapping in data tables to prevent cell clipping.
- **Interactive Feedback**:
  - Spring-animated auto-row generation in the invoice table.
  - Haptic touch cues on transaction additions and receipt prints.
  - Floating status pills for running balance alerts.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Offline-First Room SQLite Architecture**:
  - *Chosen Approach*: Complete local SQLite database via Room with Kotlin Coroutines Flow.
  - *Why*: Ensures 100% uptime in stores, warehouses, and transit where internet connection is intermittent. Merchant financial records remain strictly private on the device.
  - *Alternatives Considered*: Cloud Firestore (rejected per explicit technical specification for offline-first Room DB).

- **Decision 2: 58mm Thermal Receipt Formatting Strategy**:
  - *Chosen Approach*: ESC/POS raw command builder with 32-column text alignment and a dual software Bitmap rendering engine for Bluetooth mobile printers.
  - *Why*: 58mm pocket printers are standard for mobile vendors. Generating both ESC/POS byte streams and direct bitmap prints guarantees compatibility across any Bluetooth printer brand (Zijiang, Milestone, Xprinter, etc.).
  - *Alternatives Considered*: 80mm-only (rejected by user selection).

- **Decision 3: Voice Input Pipeline with Gemini Audio Transcription**:
  - *Chosen Approach*: Android `AudioRecord` / `MediaRecorder` encoding lightweight audio, transmitted via OkHttp/Retrofit to Gemini's transcription endpoint using `BuildConfig.GEMINI_API_KEY`.
  - *Why*: Provides near-instant, high-accuracy transcription of Arabic item names, quantities, and transaction memos without heavy local on-device ML model overhead.

- **Decision 4: Auto-Expanding Invoice Table Logic**:
  - *Chosen Approach*: Reactive list state observer in ViewModel; whenever the last row contains non-blank data (item name or price > 0), a new blank row model is automatically appended to the list.
  - *Why*: Streamlines fast retail/wholesale data entry, minimizing clicks on small touchscreens.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────┐  ┌───────────────────────┐  ┌─────────────┐ │
│  │ Invoicing Engine View │  │ Customer Ledger View  │  │ Thermal/PDF │ │
│  │ (Auto-expanding rows, │  │ (Single-row action,   │  │ Export &    │ │
│  │  voice mic dictation) │  │  running balances)    │  │ Preview     │ │
│  └───────────┬───────────┘  └───────────┬───────────┘  └──────┬──────┘ │
├──────────────┼──────────────────────────┼─────────────────────┼────────┤
│              ▼                          ▼                     ▼        │
│                        ViewModel & State Layer                         │
│   (InvoiceViewModel, CustomerViewModel, AudioTranscriptionViewModel)   │
├──────────────────────────────┬─────────────────────────────────────────┤
│                              ▼                                         │
│                       Repository Layer                                 │
│  ┌───────────────────────────┴────────────────────────────┐            │
│  │ AccountingRepository (Room DAO)                        │            │
│  │ ThermalPrinterService (ESC/POS 58mm Engine & Bluetooth)│            │
│  │ GeminiTranscriptionService (Audio Transcription)       │            │
│  │ DocumentExportService (PdfDocument & Receipt Bitmap)   │            │
│  └───────────────────────────┬────────────────────────────┘            │
├──────────────────────────────┼─────────────────────────────────────────┤
│                              ▼                                         │
│                      Local Data Storage                                │
│   Room Database: Customers, Invoices, InvoiceItems, Vouchers/Entries   │
└────────────────────────────────────────────────────────────────────────┘
```

#### Core Entities & Schema

1. **Customer (`customers`)**:
   - `id: Long` (PK autogenerate)
   - `name: String`, `phone: String`, `address: String`
   - `openingBalance: Double`, `currentBalance: Double`, `createdAt: Long`

2. **Invoice (`invoices`)**:
   - `id: Long` (PK autogenerate)
   - `invoiceNumber: String` (e.g. `INV-2026-0001`)
   - `customerId: Long?` (nullable for cash walking customers)
   - `customerName: String`
   - `paymentType: PaymentType` (`CASH` | `CREDIT`)
   - `subtotal: Double`, `discount: Double`, `tax: Double`, `grandTotal: Double`
   - `paidAmount: Double`, `remainingAmount: Double`
   - `notes: String`, `createdAt: Long`

3. **InvoiceItem (`invoice_items`)**:
   - `id: Long` (PK autogenerate)
   - `invoiceId: Long` (FK)
   - `description: String`, `quantity: Double`, `unitPrice: Double`, `totalPrice: Double`

4. **VoucherEntry (`vouchers`)**:
   - `id: Long` (PK autogenerate)
   - `customerId: Long`
   - `voucherType: VoucherType` (`RECEIPT` / قبض, `PAYMENT` / صرف, `INVOICE_CREDIT` / فاتورة آجلة)
   - `amount: Double`, `debit: Double`, `credit: Double`, `runningBalance: Double`
   - `statement: String`, `createdAt: Long`

#### Step-by-Step Implementation Sequence

1. **Dependencies & Permissions**:
   - Verify Room SQLite, Moshi/Retrofit, OkHttp in `build.gradle.kts`.
   - Add Audio recording permission (`RECORD_AUDIO`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN`) in `AndroidManifest.xml`.
   - Uncomment `GEMINI_API_KEY` in `.env.example`.
2. **Data Layer (Room Database)**:
   - Create Room Entities, DAOs (`CustomerDao`, `InvoiceDao`, `VoucherDao`), and `AppDatabase`.
   - Build `AccountingRepository` with transactions updating customer balances atomically.
3. **Core Services**:
   - `EscPos58mmPrinter`: Generates formatted byte arrays and thermal preview bitmaps (384 dots / 32 chars per line).
   - `PdfReceiptExporter`: Generates clean, vector-rendered tabular PDF invoices using Android `PdfDocument`.
   - `AudioTranscriptionService`: Records voice snippets and transcribes via Gemini (`gemini-3.5-transcribe` / `gemini-3.5-flash`).
4. **Interactive Invoicing Engine**:
   - Compose screen with 2 default rows, auto-expanding on item entry, decimal/whole quantity formatting, text wrapping, and payment linking.
5. **Customer Accounts & Ledger**:
   - Single-row horizontal action bar for receipt/payment vouchers with audio memo button.
   - Chronological ledger card list with running balance calculation.
6. **Polished App Navigation & Dashboard**:
   - Summary statistics (Total Sales, Total Debts, Active Customers), search, and fast navigation.
