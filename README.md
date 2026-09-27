# StorageIQ — Production-Grade Android Storage Intelligence

> **"Know exactly what's wasting your storage."**

StorageIQ is a premium, privacy-first Android storage intelligence application designed for users in the US, EU, UK, and worldwide. 

Unlike traditional RAM boosters or aggressive "one-click cleaners", StorageIQ behaves like a personal analyst: providing transparent, deterministic insights into duplicate media, large files, old downloads, screenshots, and messaging app media—always requiring explicit user confirmation before any destructive action is taken.

---

## Key Features

- **Storage Intelligence Dashboard:** Clear visual overview of total used storage ($117\text{ GB} / 128\text{ GB}$ used, $91\%$ full), category breakdowns, and real-time recoverable space calculations ($23.8\text{ GB}$ recoverable).
- **Exact Duplicate Detection:** High-performance, memory-efficient streaming SHA-256 duplicate engine. Clusters duplicate photos, videos, and files while **always preserving at least one original copy**.
- **Visually Similar Photo Clustering:** 2-stage candidate filtering + 64-bit `dHash` (Difference Hash) perceptual hamming distance analysis. Ranks photos by resolution ($4000\times 3000$) and sharpness to recommend the best photo to keep.
- **Large Files Explorer:** Customizable threshold filter ($>100\text{ MB}$, $>500\text{ MB}$, $>1\text{ GB}$, $>2\text{ GB}$) listing large screen recordings, downloaded movies, and camera captures.
- **Downloads Intelligence:** Categorizes old downloads ($>90\text{ days}$, $>1\text{ year}$) by file types (APKs, ZIPs, PDFs, Documents).
- **Screenshots & Screen Recordings:** Detects accumulated device screenshots and heavy screen recording videos for quick review.
- **Messaging App Media Analysis:** Analyzes WhatsApp and Telegram media directories accessible via MediaStore without inspecting private app databases.
- **App Storage Analysis:** Cleanly distinguishes App size, User Data, and Cache with direct OS settings redirects (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).
- **Historical Storage Growth Timeline:** Records daily local snapshots to track 7-day, 30-day, 90-day, and 1-year storage trends.
- **Safe & Transparent Deletion UX:** Requires explicit confirmation showing file count, total bytes, thumbnails, and destination details. Delegates to native system Trash/Recycle APIs (`MediaStore.createDeleteRequest`).
- **Structured Global Search:** Search files by name, type, size thresholds, and age criteria.

---

## Product Positioning

| Typical Phone Cleaners | StorageIQ |
|---|---|
| "Clean your phone with one tap" | "Your personal storage intelligence analyst" |
| Aggressive fake optimization claims | 100% deterministic, explainable calculations |
| Silent automatic deletion | Explicit user confirmation required for every delete |
| Intrusive ads & root/accessibility hacks | Scoped storage compliant, privacy-first & ad-free |

---

## Tech Stack

- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose + Material 3 (Emerald & Slate Theme)
- **Architecture:** Clean Architecture + MVVM
- **Dependency Injection:** Hilt
- **Navigation:** Navigation Compose
- **Local Database:** Room Database (File metadata, Duplicate clusters, Storage snapshots)
- **Preferences:** Jetpack DataStore
- **Async & Reactive:** Kotlin Coroutines + StateFlow / SharedFlow
- **Image Loading:** Coil Compose (Image & Video Thumbnail loader)
- **Background Operations:** WorkManager (`StorageScanWorker`, `PeriodicSnapshotWorker`)
- **Billing:** Google Play Billing v7+ abstractions
- **Minimum SDK:** Android 10 (API 29+) | **Target SDK:** Android 15 (API 35)

---

## Directory Layout

```
com.storage.intelligence
├── core/
│   ├── common/           # Result wrapper, Dispatchers, Byte/Date formatters, SearchFilters
│   ├── ui/               # Emerald theme, GaugeProgress, StorageBar, FileCard, SafeDeleteDialog
│   ├── database/         # Room DB, Entities (FileMetadata, Snapshots), DAOs
│   ├── datastore/        # UserPreferencesRepository
│   ├── permissions/      # PermissionManager (Android 10-15 API handling)
│   └── billing/          # Play Billing manager & SubscriptionState
├── domain/
│   ├── model/            # StorageOverview, DuplicateGroup, SimilarPhotoGroup, FileItem
│   ├── repository/       # StorageRepository, DuplicateRepository, TimelineRepository
│   └── usecase/          # ScanStorage, DetectDuplicates, DetectSimilarPhotos, DeleteSelectedFiles
├── data/
│   ├── repository/       # MediaStore + Room backed repository implementations
│   ├── scanner/          # MediaStoreScanner (incremental indexing), FileHasher (SHA-256 + dHash)
│   └── worker/           # WorkManager scan workers
└── feature/
    ├── onboarding/       # 3-slide value carousel
    ├── permissions/      # Rationale card & permission request flow
    ├── dashboard/        # Hero gauge dashboard, breakdown, insights ticker
    ├── scanner/          # Incremental scan progress visualizer
    ├── duplicates/       # Exact duplicate review list & auto-selection
    ├── similar/          # Perceptual similar photo grid & rank keeper
    ├── largefiles/       # Threshold filtered file browser
    ├── downloads/        # Download folder intelligence by age & type
    ├── screenshots/      # Screenshot & Screen recording explorer
    ├── messaging/        # WhatsApp & Telegram media analysis
    ├── apps/             # App storage & OS settings redirect
    ├── timeline/         # Storage growth trend chart & insights
    ├── cleanup/          # Central Review & Clean hub
    ├── search/           # Multi-predicate structured search
    ├── subscription/     # Pro Paywall & Play Billing integration
    └── settings/         # Privacy policy, version info & preferences
```

---

## License

Copyright © 2026 StorageIQ. All rights reserved.
