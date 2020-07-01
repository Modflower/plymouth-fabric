plugins {
    java
    `java-library`
    val loom = id("fabric-loom")
    if ("" != System.getProperty("$")) loom version System.getProperty("loom_version")!!
    `maven-publish`
}

val fabric_version: String by project
val postgres_version: String by project

group = "gay.ampflower"
version = "0.0.0"

dependencies {
    modImplementation("net.fabricmc.fabric-api", "fabric-api", fabric_version)
    include(implementation("org.postgresql", "postgresql", postgres_version))
}

minecraft {
    //accessWidener = projectDir.resolve("src/main/resources/helium.accesswidener")
}

// Required due to being a module.
// This is primarily for those who want to build this *without* building the entire stack at once.
// This also means that this project's version of Yarn, Fabric and Minecraft may not always reflect what it's developed for.
if(project.parent == null) {
    val minecraft_version: String by project
    val yarn_mappings: String by project
    val loader_version: String by project
    val jupiter_version: String by project

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        minecraft("com.mojang", "minecraft", minecraft_version)
        mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
        modImplementation("net.fabricmc", "fabric-loader", loader_version)
        testImplementation("org.junit.jupiter", "junit-jupiter-api", jupiter_version)
        testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", jupiter_version)
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
}