rootProject.name = "plymouth"

pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = java.net.URI("https://maven.fabricmc.net/")
        }
        maven {
            name = "Cotton"
            url = java.net.URI("https://server.bbkr.space/artifactory/libs-release")
        }
        gradlePluginPortal()
    }
    plugins {
        id("fabric-loom") version System.getProperty("loom_version")!!
        id("com.diffplug.spotless") version System.getProperty("spotless_version")!!
        id("com.modrinth.minotaur") version System.getProperty("minotaur_version")!!
        id("io.github.juuxel.loom-quiltflower-mini") version System.getProperty("quiltflower_version")!!
    }
}

include("utilities", "database", "ply-common", "ply-anti-xray", "ply-locking", "ply-tracker", "ply-database", "ply-debug")