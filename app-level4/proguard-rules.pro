# Level 4: Native JNI (no native obfuscation)
-allowaccessmodification
-repackageclasses ''
-overloadaggressively

-keep class com.benchmark.level4.MainActivity { *; }
-keep class com.benchmark.native_lib.NativeBridge { *; }

-dontnote **
-dontwarn **
