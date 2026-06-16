# Level 3: R8 + String Encryption + Control Flow Obfuscation
-allowaccessmodification
-repackageclasses ''
-overloadaggressively
-flattenpackagehierarchy ''
-keepattributes !LocalVariableTable,!LocalVariableTypeTable,!LineNumberTable

-keep class com.benchmark.level3.MainActivity {
    public void onCreate(android.os.Bundle);
}

-keep class com.benchmark.level3.StringDecryptor { *; }
-keep class com.benchmark.level3.ControlFlowEngine { *; }

-assumenosideeffects class android.util.Log { *; }
-optimizationpasses 5
-dontnote **
-dontwarn **
