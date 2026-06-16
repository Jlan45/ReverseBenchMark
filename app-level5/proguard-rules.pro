# Level 5: OLLVM + R8
-allowaccessmodification
-repackageclasses ''
-overloadaggressively

-keep class com.benchmark.level5.MainActivity { *; }
-keep class com.benchmark.native_lib.NativeBridge { *; }

-dontnote **
-dontwarn **
