rootProject.name = "plymouth"

pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "Quilt"
            url = uri("https://maven.quiltmc.org/repository/release/")
            content {
                includeGroup("org.quiltmc")
            }
        }
        maven {
            name = "The Glitch"
            url = uri("https://maven.ampflower.gay/")
            content {
                includeModule("gay.ampflower", "plymouth-loom")
                includeModule("plymouth-loom", "plymouth-loom.gradle.plugin")
            }
        }
        gradlePluginPortal()
    }
    plugins {
        // id("plymouth-loom") version System.getProperty("loom_version")!!
        id("fabric-loom") version System.getProperty("loom_version")!!
        id("com.diffplug.spotless") version System.getProperty("spotless_version")!!
        id("com.modrinth.minotaur") version System.getProperty("minotaur_version")!!
    }
}

include("utilities", "ply-common", "ply-anti-xray", "ply-locking", "ply-debug", "ply-utilities")

// If you want to build Tracker, uncomment the following line:
// include("database", "ply-database", "ply-tracker")