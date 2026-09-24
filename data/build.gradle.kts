plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":domain"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
