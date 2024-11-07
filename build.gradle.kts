repositories {
    mavenLocal()
    maven("https://binrepo.target.com/artifactory/maven-central")
    maven("https://binrepo.target.com/artifactory/gradle")
    maven("https://binrepo.target.com/artifactory/toolshed")
}

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    `java-library`
    `maven-publish`
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

publishing {
    repositories {
        maven {
            url = uri("https://binrepo.target.com/artifactory/product-lifecycle-automation")
            credentials(PasswordCredentials::class)
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}
