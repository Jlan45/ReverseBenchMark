# Level 2: R8 + String Encryption
-allowaccessmodification
-repackageclasses ''
-overloadaggressively
-flattenpackagehierarchy ''
-keepattributes !LocalVariableTable,!LocalVariableTypeTable,!LineNumberTable

-keep class com.benchmark.level2.MainActivity {
    public void onCreate(android.os.Bundle);
}

# Keep the string decryptor (it's called via reflection-like patterns)
-keep class com.benchmark.level2.StringDecryptor { *; }

-assumenosideeffects class android.util.Log { *; }
-optimizationpasses 5
-dontnote **
-dontwarn **
