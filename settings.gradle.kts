pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()

        maven {
            name = "spigotmc"
            url = uri("https://hub.spigotmc.org/nexus/content/groups/public/")
            content {
                includeGroup("net.md-5")
                includeGroup("org.bukkit")
                includeGroup("org.spigotmc")
            }
        }

        maven {
            name = "papermc"
            url = uri("https://artifactory.papermc.io/artifactory/universe/")
            content {
                includeGroup("com.mojang")
                includeGroupByRegex("io\\.papermc(?:\\..*)?")
                includeGroup("net.md-5")
            }
        }
    }
}

rootProject.name = "HorseMount"
