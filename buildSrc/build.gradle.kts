plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("com.android.tools.build:gradle:8.4.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
}

gradlePlugin {
    plugins {
        register("stringEncryption") {
            id = "benchmark.string-encryption"
            implementationClass = "com.benchmark.plugin.StringEncryptionPlugin"
        }
        register("controlFlow") {
            id = "benchmark.control-flow"
            implementationClass = "com.benchmark.plugin.ControlFlowPlugin"
        }
    }
}
