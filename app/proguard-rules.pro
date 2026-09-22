# ProGuard rules for Inception Android
-keep class com.inception.android.protocol.** { *; }
-keep class com.inception.android.crypto.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.bouncycastle.** { *; }

# Keep SecureIdentityStateManager from being obfuscated to prevent reflection issues
-keep class com.inception.android.identity.SecureIdentityStateManager {
    private android.content.SharedPreferences prefs;
    *;
}

# Keep reflection-accessed model packages
-keep class com.inception.android.favorites.** { *; }
-keep class com.inception.android.nostr.** { *; }
-keep class com.inception.android.identity.** { *; }
-keep class com.inception.android.model.** { *; }

# Fix for AbstractMethodError on API < 29 where LocationListener methods are abstract
-keepclassmembers class * implements android.location.LocationListener {
    public <methods>;
}
