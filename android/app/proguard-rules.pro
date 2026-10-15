# ProGuard / R8 rules for Zenith '26
-keep class com.zenith26.fest.models.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
