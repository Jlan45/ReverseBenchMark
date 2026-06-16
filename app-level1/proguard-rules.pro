# Level 1: Aggressive R8/ProGuard Obfuscation
# Maximum name obfuscation, dead code removal, optimization

-allowaccessmodification
-repackageclasses ''
-overloadaggressively
-flattenpackagehierarchy ''

# Obfuscate everything
-keepattributes !LocalVariableTable,!LocalVariableTypeTable,!LineNumberTable

# Only keep the entry point
-keep class com.benchmark.level1.MainActivity {
    public void onCreate(android.os.Bundle);
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# Aggressive optimizations
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Rename all possible classes/methods/fields
-dontnote **
-dontwarn **
