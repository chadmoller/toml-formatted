rootProject.name = "toml_formatted"

pluginManagement {
    repositories {
        maven("https://binrepo.target.com/artifactory/plugins-gradle")
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        maven("https://binrepo.target.com/artifactory/jvm-innersource")
        maven("https://binrepo.target.com/artifactory/maven-central")
    }
}
