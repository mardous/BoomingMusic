# ALAC Decoder Consumer ProGuard Rules
# These rules are included in apps that use this library

# Keep ALAC decoder classes
-keep class com.mardous.alac.** { *; }

# Keep native methods (if any are added in the future)
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
