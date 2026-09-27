# Keep Room entities & DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase

# Keep Kotlinx Serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep Hilt / Dagger
-keep class * extends dagger.hilt.internal.UnsafeCasts
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep Google Play Billing
-keep class com.android.billingclient.api.** { *; }
