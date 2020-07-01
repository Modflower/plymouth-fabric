rootProject.name = "helium"

pluginManagement {
    repositories {
        jcenter()
        maven {
            name = "Fabric"
            url = java.net.URI("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
}
