# Add project specific ProGuard rules here.
-keep class com.ibandetector.** { *; }
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn com.google.android.gms.**
