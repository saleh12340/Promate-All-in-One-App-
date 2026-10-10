# نظام الفواتير والمحاسبة الذكي (Smart Invoicing & Accounting Enterprise System)

[![Android CI](https://github.com/saleh12340/Promate-All-in-One-App-/actions/workflows/android.yml/badge.svg)](https://github.com/saleh12340/Promate-All-in-One-App-/actions/workflows/android.yml)

نظام محاسبي وإداري متطور ومستقل مخصص للأجهزة الذكية العاملة بنظام **Android**، مبني بلغة **Kotlin** بنسبة 100% وبإطار عمل **Jetpack Compose** الحديث مع معايير Material Design 3.

---

## 🌟 أبرز المميزات (Key Features)

### 1. محرك الفواتير والمبيعات التفاعلي (Invoicing Engine)
- **استغلال أعلى الشاشة للإدخال السريع**: شريط علوي مدمج لتحديد نوع الفاتورة (نقدي / آجل)، اختيار العميل أو إضافته سريعاً، ورقم الفاتورة.
- **التمدد التلقائي الذكي للأسطر**: يبدأ الجدول بسطرين افتراضيين ويتوسع تلقائياً بإضافة سطر جديد فور اكتمال تعبئة السطر الحالي.
- **تنسيق الكميات الذكي**: عرض الكميات الصحيحة كأرقام مجردة (1 أو 5 بدلاً من 1.0) مع دعم الكسور العشرية.
- **التفاف النصوص ومنع التداخل**: التفاف تلقائي للنصوص داخل خلايا البيان لمنع أي اقتطاع.
- **الربط الآلي مع حسابات العملاء**: ترحيل مديونية الفواتير الآجلة تلقائياً لحساب العميل وتحديث رصيده التراكمي.

### 2. وحدة إدارة العملاء وكشف الحساب التراكمي (Customer Ledger)
- **شريط العمليات السريع (Single-Row Action Bar)**: إدخال سندات القبض والصرف (المبلغ + البيان + زر الإضافة) في شريط أفقي موحد أعلى الشاشة.
- **كشف الحساب التراكمي المصغر**: بطاقات منظمة توضح التاريخ والتوقيت، نوع المعاملة (مدين/دائن)، البيان، و**الرصيد التراكمي المستمر (Running Balance)** بعد كل حركة.

### 3. الإدخال الصوتي والتفريغ الذكي (Gemini Audio Transcription)
- إمكانية الإدخال النصي أو الصوتي عبر الميكروفون المدمج.
- تفريغ صوتي دقيق باللغة العربية والمصطلحات التجارية باستخدام نموذج `gemini-3.5-transcribe` من Google Gemini.

### 4. محرك الطباعة والمخرجات والـ PDF
- **طباعة حرارية مباشرة 58mm**: دعم أوامر ESC/POS القياسية وطابعات البلوتوث المحمولة (32 حرفاً في السطر).
- **معاينة إيصال حراري ومشاركته كصورة**: توليد صور عالية الدقة للفواتير لمشاركتها السريعة عبر واتساب.
- **تصدير مستندات PDF رسمية**: جداول منسقة هندسياً بدقة عالية بواسطة `PdfDocument`.

### 5. الاستقلالية والأمان (Offline-First)
- حفظ محلي فوري وآمن بنسبة 100% عبر **Room Database (SQLite)** دون الحاجة للاتصال بالإنترنت.
- اعتماد نظام الأرقام الإنجليزية القياسية (0-9) والعملة الرسمية بالريال اليمني (**ر.ي / YER**).

---

## 🛠️ البنية التقنية (Tech Stack)

- **Language:** Kotlin 100%
- **UI Framework:** Jetpack Compose + Material Design 3
- **Architecture:** MVVM (Model-View-ViewModel) + StateFlow & Coroutines
- **Database:** Room Database (SQLite) + KSP
- **AI / Voice Transcription:** Gemini API (`gemini-3.5-transcribe` / `gemini-3.5-flash`)
- **Printing Engine:** ESC/POS 58mm Command Builder + Canvas Bitmap Renderer + Android PdfDocument
- **CI/CD:** GitHub Actions (`.github/workflows/android.yml`)

---

## 🚀 التشغيل والبناء (Build & Run)

### المتطلبات:
- Android Studio Ladybug أو أحدث.
- JDK 17.
- Android SDK 34+.

### أوامر البناء عبر الطرفية:
```bash
# تشغيل اختبارات الوحدة:
./gradlew testDebugUnitTest

# بناء ملف APK التجريبي:
./gradlew assembleDebug
```
مسار ملف الـ APK الناتج:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 الترخيص
تم تطوير المشروع تحت رخصة الاستخدام التجاري والتطوير المستقل.
