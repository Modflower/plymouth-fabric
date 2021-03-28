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
    testImplementation("org.junit.jupiter", "junit-jupiter-api", jupiter_version)
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", jupiter_version)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabric_api_version)
    modImplementation("me.lucko", "fabric-permissions-api", fabric_permissions_version)
}

minecraft {
    //accessWidener = projectDir.resolve("src/main/resources/helium.accesswidener")
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
    getByName<ProcessResources>("processResources") {
        inputs.property("version", project.version)

        from(sourceSets.main.get().resources.srcDirs) {
            include("fabric.mod.json")
            expand("version" to project.version,
                    "loader_version" to project.property("loader_version")?.toString(),
                    "minecraft_required" to project.property("minecraft_required")?.toString())
        }

        from(sourceSets.main.get().resources.srcDirs) {
            exclude("fabric.mod.json")
        }
    }
    withType<Jar> {
        from("LICENSE")
    }
}