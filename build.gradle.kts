plugins {
    java
    `java-library`
    id("fabric-loom")
    `maven-publish`
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val project_version: String by project

val isPublish = System.getenv("GITHUB_EVENT_NAME") == "release"
val isRelease = System.getenv("BUILD_RELEASE").toBoolean()
val isActions = System.getenv("GITHUB_ACTIONS").toBoolean()
val baseVersion: String = "$project_version+mc.$minecraft_version"
val globalVersion = when {
    isRelease -> baseVersion
    isActions -> "$baseVersion-build.${System.getenv("GITHUB_RUN_NUMBER")}-commit.${System.getenv("GITHUB_SHA").substring(0, 7)}-branch.${System.getenv("GITHUB_REF")?.substring(11)?.replace('/', '.') ?: "unknown"}"
    else -> "$baseVersion-build.local"
}

group = "gay.ampflower"
version = globalVersion

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang", "minecraft", minecraft_version)
    mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
    modImplementation("net.fabricmc", "fabric-loader", loader_version)
}

subprojects {
    if (name.startsWith("ply-")) {
        apply(plugin = "java")
        apply(plugin = "java-library")
        apply(plugin = "fabric-loom")
        apply(plugin = "maven-publish")

        group = "gay.ampflower"
        version = globalVersion

        java {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            withSourcesJar()
        }

        repositories {
            maven("https://maven.nucleoid.xyz")
        }

        dependencies {
            minecraft("com.mojang", "minecraft", minecraft_version)
            mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
            modImplementation("net.fabricmc", "fabric-loader", loader_version)
            modImplementation("xyz.nucleoid", "server-translations-api", "2.0.0-beta.2+1.19.4-pre2")
        }

        tasks {
            withType<JavaCompile> {
                options.encoding = "UTF-8"
                options.isDeprecation = true
                options.isWarnings = true
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
        }
    }
}

tasks {
    register<Copy>("poolBuilds") {
        dependsOn(build)
        if (isPublish) {
            for (p in subprojects) {
                if (p.name == "ply-debug" || !p.name.startsWith("ply-")) continue
                from(p.tasks.remapJar)
            }
        } else {
            for (p in subprojects) {
                if (p.name == "ply-debug" || !p.name.startsWith("ply-")) continue
                from(p.tasks.remapJar, p.tasks.remapSourcesJar)
            }
        }
        into(project.buildDir.resolve("pool"))
    }
}

