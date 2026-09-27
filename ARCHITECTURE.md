# Architecture & Design Specifications

StorageIQ is built adhering to **Clean Architecture** principles and **MVVM** (Model-View-ViewModel) design pattern, enforcing strict separation of concerns, provider independence, and modular maintainability.

---

## Architectural Layers

```
+-------------------------------------------------------------------+
|                        PRESENTATION LAYER                         |
|  Jetpack Compose Screens, ViewModels, Material 3 Theme, Navigation |
+-------------------------------------------------------------------+
                                  │
                                  ▼
+-------------------------------------------------------------------+
|                           DOMAIN LAYER                            |
|     Use Cases, Domain Entities, Repository Contracts, Filters     |
+-------------------------------------------------------------------+
                                  ▲
                                  │
+-------------------------------------------------------------------+
|                            DATA LAYER                             |
|  Room DAOs, MediaStoreScanner, FileHasher, WorkManager, Datastore  |
+-------------------------------------------------------------------+
```

### 1. Presentation Layer (`feature/`, `core/ui/`)
- Contains all UI composables built with **Jetpack Compose**.
- No business logic or file-system interactions reside inside composables.
- ViewModels expose reactive state via `StateFlow` using a unified `Result<T>` wrapper (`Success`, `Error`, `Loading`).

### 2. Domain Layer (`domain/`)
- Encapsulates pure business rules and data models (`FileItem`, `DuplicateGroup`, `SimilarPhotoGroup`, `StorageOverview`, `StorageSnapshot`).
- Defines repository interfaces without relying on Android framework details.
- Implements Use Cases (`ScanStorageUseCase`, `DetectDuplicatesUseCase`, `DetectSimilarPhotosUseCase`, `DeleteSelectedFilesUseCase`).

### 3. Data Layer (`data/`, `core/database/`, `core/datastore/`)
- Implements domain repository contracts (`StorageRepositoryImpl`, `DuplicateRepositoryImpl`, `TimelineRepositoryImpl`, `SubscriptionRepositoryImpl`).
- **MediaStoreScanner:** Efficiently queries `MediaStore.Files`, `MediaStore.Images`, `MediaStore.Video`, and `MediaStore.Audio` with incremental fingerprinting (skipping unchanged files based on size + path + modified time).
- **FileHasher:** Streaming SHA-256 calculator (using 8KB byte buffers to prevent memory spikes) and 64-bit `dHash` (Difference Hash) perceptual hashing engine for visual photo similarity.
- **Room Database:** Caches indexed file metadata (`FileMetadataEntity`), duplicate clusters (`DuplicateGroupEntity`), and historical storage snapshots (`StorageSnapshotEntity`).

---

## Incremental Scanning Engine

To support devices with hundreds of thousands of media files without freezing the UI or draining battery:

```
MediaStore Query -> Extract File Fingerprint (Path + Size + ModifiedTime)
                             │
            ┌────────────────┴────────────────┐
            ▼                                 ▼
   Matches Room Cache?               Modified or New File?
            │                                 │
     (Reuse Cached)                 (Classify & Index)
            │                                 │
            └────────────────┬────────────────┘
                             ▼
                Batch Insert to Room DB (250 items)
```

---

## Duplicate & Similar Photo Algorithms

### 1. Exact Duplicate Hashing Flow
1. Query Room DB for candidate files sharing identical byte sizes (`COUNT(*) > 1`).
2. Compute streaming SHA-256 hash incrementally via 8KB chunks.
3. Group matching SHA-256 strings into `DuplicateGroup` objects.
4. Auto-select copy candidates while **always preserving the newest/highest resolution file as the original**.

### 2. Visually Similar Photo (2-Stage Filter)
- **Stage 1 (Cheap Candidate Filter):** Filter photos by aspect ratio, size range, and creation timestamp proximity.
- **Stage 2 (64-bit dHash Perceptual Hashing):**
  - Scale image down to a $9\times 8$ grayscale matrix.
  - Compare adjacent horizontal pixels ($P_{x,y} < P_{x+1,y}$).
  - Construct a 64-bit `Long` hash.
  - Calculate Hamming distance: $\text{distance} \le 10$ bits out of 64 indicates visual similarity.
  - Rank candidates by resolution ($w \times h$) and sharpness to recommend the best copy to keep.
