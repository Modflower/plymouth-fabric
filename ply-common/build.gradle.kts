import java.net.URI

plugins {
    java
    `java-library`
    id("fabric-loom")
    `maven-publish`
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val jupiter_version: String by project

val isRelease = System.getenv("BUILD_RELEASE").toBoolean()
val isActions = System.getenv("GITHUB_ACTIONS").toBoolean()
val baseVersion: String = "${project.property("project_version")}+minecraft.$minecraft_version"

group = "gay.ampflower"
version = when {
    isRelease -> baseVersion
    isActions -> "$baseVersion+build.${System.getenv("GITHUB_RUN_ID")}+commit.${System.getenv("GITHUB_SHA").substring(0, 7)}+branch.${System.getenv("GITHUB_REF")?.substring(11)?.replace('/', '-') ?: "unknown"}"
    else -> "$baseVersion+build.local"
}

repositories {
    maven { url = URI.create("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    minecraft("com.mojang", "minecraft", minecraft_version)
    mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
    modImplementation("net.fabricmc", "fabric-loader", loader_version)
}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isDeprecation = true
        options.isWarnings = true
    }
    register<Jar>("sourcesJar") {
        dependsOn("classes")
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to project.version,
                "loader_version" to project.property("loader_version")?.toString(),
                "minecraft_required" to project.property("minecraft_required")?.toString()
            )
        }
    }
    withType<Jar> {
        from("LICENSE")
    }
}