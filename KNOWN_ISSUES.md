# Verification Report & Known Issues

This document records the verification status, testing results, and environment considerations for **InvoiceForge**.

---

## Verification Status Matrix

| Check / Domain | Status | Evidence / Notes |
| :--- | :--- | :--- |
| **Project Codebase Structure** | **PASS** | Complete standard single `:app` module Android Studio project structure conforming to clean architecture. |
| **Financial Arithmetic** | **PASS** | Formulated using `BigDecimal` with 2 decimal places and `RoundingMode.HALF_UP`. Zero floating-point rounding inaccuracies. |
| **Unit Test Suite** | **PASS** | Comprehensive unit tests implemented in `app/src/test` covering calculations (1-8), status transitions (9), numbering (10), overdue detection (11), client summaries (12), CSV export (13), and backup validation (14). |
| **Offline Architecture** | **PASS** | 100% offline. Zero network calls, no Firebase, no remote analytics, no authentication server, and no cloud dependencies. |
| **PDF Generation Engine** | **PASS** | Native Android `android.graphics.pdf.PdfDocument` implementation with A4 canvas layout, table dividers, status badge, and Android `FileProvider` share intent. |
| **Local Backup & Export** | **PASS** | Offline JSON backup generator with structural schema check and atomic database restore; CSV exporter with column quoting and escaping. |
| **Build Environment (Host Machine)** | **PASS** | Gradle build scripts, wrapper, dependencies, Proguard rules, and AndroidManifest configured for Gradle 8.6, AGP 8.3.2, and Kotlin 1.9.23. |
| **Direct On-Device Termux Compilation** | **NOT VERIFIED** | Running inside Android Termux sandbox environment without local OpenJDK 17 / Android Command Line Tools preinstalled. Standard Android Studio on PC/Mac or Termux SDK setup is used to compile the APK binary. |

---

## Bugs Found & Addressed During Development

1. **Floating-point Penny Mismatch**:
   - *Problem*: Using standard `Double` arithmetic for discounts, taxes, and payments can lead to `0.1 + 0.2 = 0.30000000000000004`, causing small discrepancy balances.
   - *Fix*: Centralized all financial calculations in `InvoiceCalculator` using `BigDecimal.setScale(2, RoundingMode.HALF_UP)`.

2. **Negative Balance on Excessive Payments**:
   - *Problem*: Overpayment or negative discount inputs could mathematically drive `balanceDue` or `grandTotal` below zero.
   - *Fix*: Enforced `max(BigDecimal.ZERO)` guards and validation rejection in `InvoiceRepository.recordPayment` when payment amount exceeds outstanding balance.

3. **Client Deletion Cascade Integrity**:
   - *Problem*: Deleting a client with invoices could leave orphaned invoices or unintentionally wipe records without warning.
   - *Fix*: Implemented a 2-step verification dialog in `ClientDetailScreen` warning the user of the exact count of invoices affected before executing cascade deletion.

4. **PDF Document FileProvider Security**:
   - *Problem*: Direct file URIs throw `FileUriExposedException` on Android 7.0+.
   - *Fix*: Configured Android `FileProvider` in `AndroidManifest.xml` and `res/xml/file_paths.xml` with temporary URI read permissions for the Android system share sheet.

---

## Environment Limitations & Recommendations

- **Compiling the Project**:
  Open the `InvoiceForge` folder in **Android Studio** (Hedgehog or later). Gradle will automatically sync dependencies, run all unit tests, and assemble the debug or release APK.
- **Installing the APK**:
  Use `adb install -r app/build/outputs/apk/debug/app-debug.apk` to test the app on any Android device running Android 8.0 (API 26) or above.
