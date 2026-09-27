# Privacy Architecture & Data Protection Guarantees

StorageIQ is built from the ground up on the fundamental principle of **100% On-Device Storage Intelligence**.

---

## Core Privacy Principles

1. **Zero Cloud Uploads for Storage Analysis:**
   - All file scanning, SHA-256 duplicate hashing, perceptual `dHash` photo calculations, and storage growth logging execute exclusively on your local device CPU and storage hardware.
   - Your photos, videos, filenames, personal documents, and messaging media never leave your phone.

2. **No Data Mining or Analytics Tracking of Files:**
   - Product telemetry (if enabled) tracks only high-level UI events (`scan_completed`, `category_opened`, `cleanup_completed`).
   - Personal file paths, media content, or filenames are strictly excluded from analytics logs.

3. **No Intrusive Permissions:**
   - StorageIQ **NEVER** requests root privileges, Accessibility Services, SMS, contacts, call logs, location, or device administrator rights.
   - We query only official Android `MediaStore` content APIs.

4. **Explicit User Consent & Safe Deletion:**
   - StorageIQ never deletes user data automatically. Every file deletion requires explicit user selection and system dialog confirmation (`MediaStore.createDeleteRequest`).
