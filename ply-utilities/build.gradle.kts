import java.net.URI

plugins {
    java
    `java-library`
    id("fabric-loom")
    `maven-publish`
}

val fabric_api_version: String by project
val fabric_permissions_version: String by project

repositories {
    mavenCentral()
    maven { url = URI.create("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    implementation(project(":ply-common", configuration = "namedElements"))
    modImplementation("me.lucko", "fabric-permissions-api", fabric_permissions_version)
    modRuntimeOnly(fabricApi.module("fabric-resource-loader-v0", fabric_api_version))
}
