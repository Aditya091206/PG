# ProGuard and R8 rules for PG Manager Production

# Keep Data Models and Firebase entities
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Firebase Firestore and Auth models
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}
-keep class com.pgmanager.app.domain.model.** { *; }

# Keep ZXing QR generation
-keep class com.google.zxing.** { *; }

# Keep Coil image loader
-keep class coil.** { *; }

# Keep Android Security Crypto
-keep class androidx.security.crypto.** { *; }

# Suppress warnings from common libraries
-dontwarn okio.**
-dontwarn com.google.errorprone.annotations.**
