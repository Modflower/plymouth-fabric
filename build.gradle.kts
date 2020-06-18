group = "net.kjp12"
version = "0.0.0"

dependencies {
    modImplementation("net.fabricmc.fabric-api", "fabric-api", property("fabric_version")?.toString())
    implementation("org.postgresql", "postgresql", property("postgres_version")?.toString())
}

minecraft {
    //accessWidener = projectDir.resolve("src/main/resources/helium.accesswidener")
}
/*
// Required due to being a module.
// This is primarily for those who want to build this *without* building the entire stack at once.
// This also means that this project's version of Yarn, Fabric and Minecraft may not always reflect what it's developed for.
if(project.parent == null) {
    plugins {
        java
        `java-library`
        id("fabric-loom") version "0.2.7-SNAPSHOT"
        `maven-publish`
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        minecraft("com.mojang", "minecraft", property("minecraft_version")?.toString())
        mappings("net.fabricmc", "yarn", property("yarn_mappings")?.toString(), classifier="v2")
        modImplementation("net.fabricmc", "fabric-loader", property("loader_version")?.toString())
        testImplementation("org.junit.jupiter", "junit-jupiter-api", property("jupiter_version")?.toString())
        testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", property("jupiter_version")?.toString())
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