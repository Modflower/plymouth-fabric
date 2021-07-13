import com.modrinth.minotaur.TaskModrinthUpload
import java.net.URI

plugins {
    java
    `java-library`
    id("fabric-loom")
    id("com.modrinth.minotaur")
    `maven-publish`
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_api_version: String by project
val modrinth_id: String by project
val project_version: String by project

val isRelease = System.getenv("BUILD_RELEASE").toBoolean()
val isActions = System.getenv("GITHUB_ACTIONS").toBoolean()
val baseVersion: String = "$project_version+mc.$minecraft_version"

group = "net.kjp12"
version = when {
    isRelease -> baseVersion
    isActions -> "$baseVersion-build.${System.getenv("GITHUB_RUN_NUMBER")}-commit.${System.getenv("GITHUB_SHA").substring(0, 7)}-branch.${System.getenv("GITHUB_REF")?.substring(11)?.replace('/', '.') ?: "unknown"}"
    else -> "$baseVersion-build.local"
}

repositories {
    maven { url = URI.create("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = URI.create("https://maven.dblsaiko.net") }
}

dependencies {
    minecraft("com.mojang", "minecraft", minecraft_version)
    mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
    modImplementation("net.fabricmc", "fabric-loader", loader_version)
    modRuntime(fabricApi.module("fabric-resource-loader-v0", fabric_api_version))
    // Hi, yes, we're very much up to no good here. Good luck, Minecraft!
    modImplementation("net.gudenau.minecraft", "gudasm", "0.2.10")
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
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
        val map = mapOf(
            "version" to project.version,
            "project_version" to project_version,
            "loader_version" to loader_version,
            "minecraft_required" to project.property("minecraft_required")?.toString()
        )
        inputs.properties(map)

        filesMatching("fabric.mod.json") {
            expand(map)
        }
    }
    withType<Jar> {
        from("LICENSE")
    }
    register<TaskModrinthUpload>("publishModrinth") {
        token = System.getenv("MODRINTH_TOKEN")
        projectId = modrinth_id
        versionNumber = version.toString()
        releaseType = System.getenv("RELEASE_OVERRIDE") ?: if (isRelease) "release" else "beta"
        uploadFile = remapJar
        addGameVersion(minecraft_version)
        addLoader("fabric")
    }
}