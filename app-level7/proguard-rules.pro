# Level 7: VMP + OLLVM + Anti-debug + R8 (maximum protection)
-allowaccessmodification
-repackageclasses ''
-overloadaggressively
-flattenpackagehierarchy ''
-keepattributes !LocalVariableTable,!LocalVariableTypeTable,!LineNumberTable

-keep class com.benchmark.level7.MainActivity { *; }
-keep class com.benchmark.native_lib.NativeBridge { *; }

-assumenosideeffects class android.util.Log { *; }
-optimizationpasses 5
-dontnote **
-dontwarn **
