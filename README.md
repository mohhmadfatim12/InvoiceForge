# InvoiceForge — Offline Invoice Maker & PDF Generator

> **100% Offline • Zero Accounts • No Subscriptions • Professional A4 Invoices**

InvoiceForge is a modern, privacy-focused, offline-first Android application designed for freelancers, independent contractors, small business owners, and service providers. It empowers professionals to create custom invoices, track payments, generate print-ready A4 PDF invoices, and manage client databases without needing an internet connection, account registration, or recurring fees.

---

## Key Features

### 🏢 Business Profile & Branding
- Set business name, address, contact details, currency, default tax rates, and standard payment terms.
- Automatic branding and layout on every generated invoice.
- Multi-currency support (USD, PKR, EUR, GBP, INR, CAD, AUD, AED, SAR, and custom symbols).

### 👥 Client Relationship Management
- Add, edit, view, search, and manage clients.
- Tracks contact details, physical address, and private client notes.
- Client summary showing lifetime invoiced amount, total paid, and current outstanding balance.
- Safety guardrails preventing accidental client deletion if invoices exist without explicit confirmation.

### 🧾 Smart Invoice Management
- Auto-incrementing invoice numbering (`INV-0001`, `INV-0002`, etc.) with customizable prefix.
- Add unlimited line items with quantity, unit price, and optional per-item discounts.
- Flexible discounts and taxes (percentage-based or fixed amounts).
- Optional shipping and freight fees.
- Real-time subtotal, discount, tax, and grand-total calculation.
- Dynamic status tracking: **Draft**, **Pending**, **Partially Paid**, **Paid**, and **Overdue**.
- Automatic overdue detection based on due dates and balance.
- One-tap invoice duplication to fast-track repeat billing.

### 💳 Payment Tracking
- Record full or partial payments with amount, payment method (Cash, Bank Transfer, Card, Cheque, Other), and optional notes.
- Prevents payment amounts from exceeding the remaining balance.
- Complete payment ledger per invoice.

### 📄 Offline A4 PDF Invoice Generation
- Generates pixel-perfect, standards-compliant A4 PDF documents directly on the device using Android's native `android.graphics.pdf.PdfDocument` API.
- Clean commercial layout: business header, metadata, client details, line items table, financial breakdown, notes, and payment instructions.
- Instant PDF preview and sharing via Android's native system share sheet (WhatsApp, Email, Drive, Print, etc.) via secure `FileProvider`.

### 📊 Financial Dashboard & Analytics
- Visual overview of Total Invoiced, Received, Outstanding, and Overdue balances.
- Invoices created this month.
- 6-month visual revenue comparison chart (Invoiced vs. Paid).
- Recent invoice list for quick access.

### 💾 Data Portability & Safety
- **CSV Export:** Export all invoices and client lists to standard CSV files for Excel, Numbers, or Google Sheets.
- **JSON Backup & Restore:** 100% offline JSON backup export and validated restore with structural versioning checks and safety confirmations.
- All data stored locally in SQLite via Room Database.

---

## Architecture & Code Structure

InvoiceForge strictly follows clean architecture principles with unidirectional data flow (UDF):

```text
com.invoiceforge.app/
├── data/
│   ├── backup/          # JSON Backup serialization, schema validation, and restore
│   ├── export/          # CSV Exporter for invoices and client databases
│   ├── local/
│   │   ├── dao/         # Room DAOs (BusinessProfile, Client, Invoice, Item, Payment)
│   │   ├── database/    # Room Database with seed callback and TypeConverters
│   │   ├── entity/      # Room Entities and relational composite models
│   │   └── preferences/ # DataStore Preferences (Theme, Prefix)
│   ├── pdf/             # Offline A4 canvas PDF generation & FileProvider sharing
│   └── repository/      # Business logic, reactive Flow data sources, and calculations
├── di/                  # AppContainer offline service locator
├── domain/
│   ├── calculator/      # Precision BigDecimal financial math & overdue/numbering engine
│   └── model/           # Status enums, currency definitions, and dashboard aggregates
├── ui/
│   ├── components/      # StatusBadge, EmptyStateView, StatCard, ConfirmationDialog
│   ├── dashboard/       # DashboardScreen and analytics ViewModel
│   ├── invoices/        # List, Add/Edit, Detail, and PDF Preview screens
│   ├── clients/         # Client List, Add/Edit, and Detail screens
│   ├── navigation/      # Jetpack Navigation Compose graph and bottom navigation
│   ├── settings/        # Business profile, theme, and data management screen
│   └── theme/           # Material 3 Color palette, typography, and theme definitions
├── InvoiceForgeApplication.kt
└── MainActivity.kt
```

---

## Technology Stack

- **Language:** Kotlin 1.9.23
- **UI Framework:** Jetpack Compose with Material 3
- **Local Database:** Room Database 2.6.1 with KSP
- **Asynchronous Execution:** Kotlin Coroutines & Flow
- **Preferences:** AndroidX DataStore Preferences 1.0.0
- **Navigation:** Navigation Compose 2.7.7
- **PDF Engine:** Android Native `PdfDocument` (zero external PDF binary dependencies)
- **Financial Arithmetic:** Java `BigDecimal` with `RoundingMode.HALF_UP` (zero floating-point errors)
- **Unit Testing:** JUnit 4, Kotlinx Coroutines Test, AndroidX Core Testing
- **Minimum SDK:** 26 (Android 8.0 Oreo)
- **Target SDK:** 34 (Android 14)

---

## Build & Testing Instructions

### Prerequisites
- Android Studio Hedgehog / Iguana / Jellyfish (or higher)
- JDK 17
- Android SDK Platform 34

### Building the Debug APK
```bash
# Clone or navigate to the repository
cd InvoiceForge

# Assemble the debug APK
./gradlew clean assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Installing and Launching on Mobile Device via ADB
Connect your Android phone via USB with USB Debugging enabled (or Wireless Debugging):
```bash
# Verify connected device
adb devices

# Install APK to device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch the app directly
adb shell am start -n com.invoiceforge.app/.MainActivity
```

---

## License

InvoiceForge is licensed under the Apache License 2.0. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for licenses of incorporated libraries.
