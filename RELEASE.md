# Release & Google Play Store Deployment Guide

This document outlines the signing, obfuscation, and deployment procedures for releasing StorageIQ on the Google Play Store.

---

## 1. Signing Configuration

Do **NOT** commit keystore files or passwords into Git. Store keystore secrets in your CI environment variables (`KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_PASSWORD`).

In `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 2. ProGuard & R8 Optimization Rules

Obfuscation is enabled for release builds. Pre-configured rules in `app/proguard-rules.pro` preserve Room DAOs, Kotlinx Serialization serializers, Hilt dependency injection graphs, and Google Play Billing APIs.

---

## 3. Google Play Store Compliance Checklist

- [x] Scoped Storage compliant (Target SDK 35).
- [x] Minimum permissions declared (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`).
- [x] Clear trial & auto-renewal subscription disclosures.
- [x] On-device privacy policy linked.
- [x] No fake optimization claims or performance guarantees.
