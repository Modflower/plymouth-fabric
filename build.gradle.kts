import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

plugins {
    java
    `java-library`
    id("fabric-loom")
    `maven-publish`
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_api_version: String by project
val fabric_permissions_version: String by project

group = "gay.ampflower"
version = "0.0.0"

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    maven { url = URI.create("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    minecraft("com.mojang", "minecraft", minecraft_version)
    mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
    modImplementation("net.fabricmc", "fabric-loader", loader_version)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabric_api_version)
    modImplementation("me.lucko", "fabric-permissions-api", fabric_permissions_version)
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
        inputs.property("version", project.version)

        from(sourceSets.main.get().resources.srcDirs) {
            include("fabric.mod.json")
            expand(
                "version" to project.version,
                "loader_version" to project.property("loader_version")?.toString(),
                "minecraft_required" to project.property("minecraft_required")?.toString()
            )
        }

        from(sourceSets.main.get().resources.srcDirs) {
            exclude("fabric.mod.json")
        }
    }
    withType<Jar> {
        from("LICENSE")
    }
    register<Copy>("poolBuilds") {
        for (p in subprojects) {
            if (p.name == "ply-debug") continue
            from(p.tasks.jar, p.tasks.remapJar, p.task("sourcesJar"), p.tasks.remapSourcesJar)
        }
        from(jar, remapJar, sourcesJar, remapSourcesJar)
        into(project.buildDir.resolve("pool"))
    }
    register<Task>("uploadPool") {
        dependsOn(build)
        for (p in subprojects) {
            if (p.name == "ply-debug") continue
            dependsOn(p.tasks.build)
        }
        // I can guarantee you that the following was never supposed to happen.
        doLast {
            val gson = Gson()
            val httpClient = HttpClient.newHttpClient()
            val urir =
                "${System.getenv("ACTIONS_RUNTIME_URL")}_apis/pipelines/workflows/${System.getenv("GITHUB_RUN_ID")}/artifacts?api-version=6.0-preview"
            println(urir)
            val uri =
                URI.create(urir)
            project.logger.debug("POST {}", uri)
            val request = HttpRequest.newBuilder(uri).header("Content-Type", "application/json")
                .header("Tiny-Potato", "Hi GitHub!")
                .POST(HttpRequest.BodyPublishers.ofString("{\"Type\":\"actions_storage\",\"Name\":\"potato-pool\"}"))
                .build()
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() / 100 != 2) {
                project.logger.warn("Unable to continue. {}\n\n{}", response.statusCode(), response.body())
            } else {
                val containerUrl =
                    gson.fromJson(response.body(), JsonObject::class.java)["fileContainerResourceUrl"].asString
                val set = hashSetOf(
                    jar.get().archiveFile.get().asFile,
                    remapJar.get().archiveFile.get().asFile,
                    sourcesJar.get().archiveFile.get().asFile,
                    remapSourcesJar.get().output
                )

                for (p in subprojects) {
                    if (p.name == "ply-debug") continue
                    set.add(p.tasks.jar.get().archiveFile.get().asFile)
                    set.add(p.tasks.remapJar.get().archiveFile.get().asFile)
                    set.add(p.task<Jar>("sourcesJar").archiveFile.get().asFile)
                    set.add(p.tasks.remapSourcesJar.get().output)
                }

                for (file in set) {
                    val urr = containerUrl + "?itemPath=${file.name}"
                    println(urr)
                    val ur = URI.create(urr)
                    val req = HttpRequest.newBuilder(ur).header("Content-Type", "application/octet-stream")
                        .PUT(HttpRequest.BodyPublishers.ofFile(file.toPath())).build()
                    httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofString()).handleAsync { res, thr ->
                        if (thr != null) {
                            project.logger.warn("Failed to upload {}", ur, thr)
                        } else if (res.statusCode() / 100 != 2) {
                            project.logger.warn(
                                "Failed to upload {} for {}\n\n{}",
                                arrayOf(ur, res.statusCode(), res.body())
                            )
                        }
                    }
                }
            }
        }
    }
}