# Level 8: VMP + Encrypted Bytecode + OLLVM + Anti-debug + R8
-allowaccessmodification
-repackageclasses ''
-overloadaggressively
-flattenpackagehierarchy ''
-keepattributes !LocalVariableTable,!LocalVariableTypeTable,!LineNumberTable

-keep class com.benchmark.level8.MainActivity { *; }
-keep class com.benchmark.native_lib.NativeBridge { *; }

-assumenosideeffects class android.util.Log { *; }
-optimizationpasses 5
-dontnote **
-dontwarn **
