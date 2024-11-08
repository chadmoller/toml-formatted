repositories {
    mavenLocal()
    mavenCentral()
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    `java-library`
    alias(libs.plugins.spotless)
}

group = "product-lifecycle-automation"

dependencies {
    api(libs.kotlinDate)
    api(libs.arrowKt)

    testImplementation(libs.bundles.testing)
}

val jvmTargetVersion: String by project

tasks {
    java { toolchain { languageVersion.set(JavaLanguageVersion.of(jvmTargetVersion)) } }

    withType<Test> {
        useJUnitPlatform()
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
