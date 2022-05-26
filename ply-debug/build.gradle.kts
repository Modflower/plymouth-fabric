plugins {
    java
    `java-library`
    id("fabric-loom")
    `maven-publish`
}

val jupiter_version: String by project
val fabric_api_version: String by project
val fabric_permissions_version: String by project

repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    modImplementation("com.github.Modflower", "bytecode-junkie", "v0.3.0")
    implementation(project(":utilities"))
    // implementation(project(":database"))
    implementation(project(":ply-utilities", configuration = "namedElements"))
    implementation(project(":ply-anti-xray", configuration = "namedElements"))
    implementation(project(":ply-common", configuration = "namedElements"))
    // implementation(project(":ply-database"))
    implementation(project(":ply-locking", configuration = "namedElements"))
    // implementation(project(":ply-tracker"))
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabric_api_version)
    modImplementation("me.lucko", "fabric-permissions-api", fabric_permissions_version)
}