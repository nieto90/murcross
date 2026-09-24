plugins {
    id("org.jetbrains.kotlin.jvm")
}
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":domain"))
    testImplementation(project(":data"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
