# Android Permission Rationale & API Compliance

StorageIQ adheres strictly to Google Play Developer Policies and Scoped Storage requirements on Android 10+ (API 29+).

---

## Requested Permissions Matrix

| Permission | Android Version | Purpose & Rationale |
|---|---|---|
| `READ_MEDIA_IMAGES` | Android 13+ (API 33+) | Allows user-initiated scanning of image library to detect duplicates, similar photos, and screenshots. |
| `READ_MEDIA_VIDEO` | Android 13+ (API 33+) | Allows user-initiated scanning of video files to detect duplicate videos and screen recordings. |
| `READ_MEDIA_AUDIO` | Android 13+ (API 33+) | Allows scanning audio files in public Downloads and Messaging folders. |
| `READ_EXTERNAL_STORAGE` | Android 10–12 (API 29–32) | Legacy storage permission for media analysis on older Android versions. |
| `POST_NOTIFICATIONS` | Android 13+ (API 33+) | Optional permission to alert the user when background storage analysis completes. |

---

## Scoped Storage & File Access Compliance

- **No All Files Access (`MANAGE_EXTERNAL_STORAGE`):** StorageIQ queries standard public media collections exposed via Android `MediaStore`. We do not request broad root filesystem access.
- **System-Supported Deletion:** File removals on Android 11+ delegate directly to `MediaStore.createDeleteRequest(contentResolver, uris)`. This triggers the native Android OS confirmation prompt and moves items to system Trash.
- **Graceful Degradation:** If permissions are denied or revoked, StorageIQ remains functional with basic UI guides and explicit system settings prompts (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).
