# Build & Environment Setup Instructions

Follow these steps to build, test, and run StorageIQ locally in Android Studio or via Gradle CLI.

---

## Prerequisites

- **Android Studio:** Ladybug (2024.2.1+) or newer.
- **JDK:** Java 21 LTS (Corretto, Temurin, or Oracle OpenJDK 21).
- **Android SDK:** API Level 35 (Android 15) with Build Tools 35.0.0 installed.

---

## Local Configuration

1. Clone repository to your local workspace:
   ```bash
   git clone https://github.com/your-org/storage-iq.git
   cd storage-iq
   ```

2. Create a `local.properties` file in the root project directory:
   ```properties
   sdk.dir=C:/Users/YOUR_USERNAME/AppData/Local/Android/Sdk
   ```

---

## Command Line Build & Testing

- **Run All Unit Tests:**
  ```bash
  ./gradlew test
  ```

- **Build Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```

- **Build Release Android App Bundle (AAB):**
  ```bash
  ./gradlew bundleRelease
  ```
