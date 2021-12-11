import java.net.URI

plugins {
    java
    `java-library`
    id("fabric-loom")
    `maven-publish`
    id("io.github.juuxel.loom-quiltflower-mini")
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_api_version: String by project
val fabric_permissions_version: String by project
val project_version: String by project

val isPublish = System.getenv("GITHUB_EVENT_NAME") == "release"
val isRelease = System.getenv("BUILD_RELEASE").toBoolean()
val isActions = System.getenv("GITHUB_ACTIONS").toBoolean()
val baseVersion: String = "$project_version+mc.$minecraft_version"

group = "net.kjp12"
version = when {
    isRelease -> baseVersion
    isActions -> "$baseVersion-build.${System.getenv("GITHUB_RUN_NUMBER")}-commit.${System.getenv("GITHUB_SHA").substring(0, 7)}-branch.${System.getenv("GITHUB_REF")?.substring(11)?.replace('/', '.') ?: "unknown"}"
    else -> "$baseVersion-build.local"
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

repositories {
    mavenCentral()
    maven { url = URI.create("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    minecraft("com.mojang", "minecraft", minecraft_version)
    mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
    modImplementation("net.fabricmc", "fabric-loader", loader_version)
    modImplementation("me.lucko", "fabric-permissions-api", fabric_permissions_version)
    modRuntime(fabricApi.module("fabric-resource-loader-v0", fabric_api_version))
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isDeprecation = true
        options.isWarnings = true
    }
    val sourcesJar = register<Jar>("sourcesJar") {
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
    register<Copy>("poolBuilds") {
        dependsOn(build)
        if (isPublish) {
            for (p in subprojects) {
                if (p.name == "ply-debug" || !p.name.startsWith("ply-")) continue
                from(p.tasks.remapJar)
            }
            from(remapJar)
        } else {
            for (p in subprojects) {
                if (p.name == "ply-debug" || !p.name.startsWith("ply-")) continue
                from(p.tasks.jar, p.tasks.remapJar, p.tasks.getByName("sourcesJar"), p.tasks.remapSourcesJar)
            }
            from(jar, remapJar, sourcesJar, remapSourcesJar)
        }
        into(project.buildDir.resolve("pool"))
    }
}

