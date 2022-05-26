plugins {
    java
    `java-library`
    id("fabric-loom")
    `maven-publish`
}

val postgres_version: String by project
val fabric_api_version: String by project

dependencies {
    api(project(":ply-common"))
    api(project(":database")) { include(this) }
    include(implementation("org.postgresql", "postgresql", postgres_version))
    modRuntimeOnly(fabricApi.module("fabric-resource-loader-v0", fabric_api_version))
}