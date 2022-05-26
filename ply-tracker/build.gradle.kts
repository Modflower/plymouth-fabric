plugins {
    java
    `java-library`
    id("fabric-loom")
    `maven-publish`
}

val fabric_api_version: String by project
val fabric_permissions_version: String by project

repositories {
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    modImplementation(fabricApi.module("fabric-command-api-v1", fabric_api_version)) { include(this) }
    implementation(project(":ply-database", configuration = "namedElements"))
    implementation(project(":utilities")) { include(this) }
    implementation(project(":database"))
    // implementation(project(":commander")) { include(this) }
    modImplementation("me.lucko", "fabric-permissions-api", fabric_permissions_version)
}