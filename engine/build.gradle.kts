plugins {
    id("org.jetbrains.kotlin.jvm")
}
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation(project(":domain"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.json:json:20240303")
}
tasks.test { useJUnitPlatform() }
