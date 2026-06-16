# Level 6: OLLVM + Anti-debug + R8
-allowaccessmodification
-repackageclasses ''
-overloadaggressively

-keep class com.benchmark.level6.MainActivity { *; }
-keep class com.benchmark.native_lib.NativeBridge { *; }

-dontnote **
-dontwarn **
