import com.modrinth.minotaur.TaskModrinthUpload
import com.modrinth.minotaur.request.VersionType
import net.fabricmc.mapping.tree.TinyMappingFactory
import net.fabricmc.mapping.tree.TinyTree
import java.net.URI
import java.util.zip.ZipFile
import java.util.Properties as JavaProp

plugins {
    java
    `java-library`
    id("plymouth-loom")
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
    maven { url = URI.create("https://jitpack.io") }
}

dependencies {
    minecraft("com.mojang", "minecraft", minecraft_version)
    mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
    modImplementation("net.fabricmc", "fabric-loader", loader_version)
    modRuntime(fabricApi.module("fabric-resource-loader-v0", fabric_api_version))
    // Hi, yes, we're very much up to no good here. Good luck, Minecraft!
    include(modImplementation("com.github.the-glitch-network", "minecraft-gudasm", "v0.3.0"))
    api(project(":utilities")) { include(this) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// https://discuss.gradle.org/t/get-a-path-to-dependencies-jar-file/5084
val yarn = configurations.mappings.get().resolvedConfiguration.resolvedArtifacts.find { it.moduleVersion.id.name == "yarn" }!!
val tinyTree: TinyTree by lazy { ZipFile(yarn.file).use { it.getInputStream(it.getEntry("mappings/mappings.tiny")).bufferedReader().use(TinyMappingFactory::load) } }
val tinyNamedToIntermediaryMap: Map<String, String> by lazy {
    val map = HashMap<String, String>()
    for (clazz in tinyTree.classes) map[clazz.getName("named")] = clazz.getName("intermediary")
    map
}

fun transformer(it: String): String {
    val sb = StringBuilder()
    val isDesc = it.startsWith(';')
    // Required for as there is no other way for the reader to differentiate between descriptors and internal names.
    if (isDesc) sb.append(';')
    for (clazz in it.split(';')) {
        if (clazz.isEmpty()) continue
        val remap = if (isDesc) {
            val substr = clazz.indexOf('L') + 1
            sb.append(clazz.substring(0, substr))
            clazz.substring(substr)
        } else clazz
        sb.append(tinyNamedToIntermediaryMap[remap]).append(';')
    }
    return sb.toString()
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
    val scourPackages = register<Task>("scourPackages") {
        val asmFile = projectDir.resolve("asm.properties")
        val asmDir = buildDir.resolve("asm")

        inputs.property("yarn", yarn.moduleVersion.id)
        inputs.file(asmFile)
        outputs.dir(asmDir).withPropertyName("outputDir")

        doLast {
            val map = HashMap<String, Pair<String, ArrayList<String>>>()
            val asm = JavaProp()
            asmFile.inputStream().use(asm::load)
            for ((k, v) in asm) map[v as String] = k as String to ArrayList()

            for (clazz in tinyTree.classes) {
                clazz.getName("named").let {
                    for ((packet, value) in map) if (it.startsWith(packet)) value.second.add(it)
                }
            }

            for ((packet, list) in map.values) {
                asmDir.resolve("$packet.sys").bufferedWriter(bufferSize = 8096).use {
                    for (clazz in list) it.append(clazz).append(';')
                }
            }
        }
    }
    processResources {
        dependsOn(scourPackages)
        val map = mapOf(
            "version" to project.version,
            "project_version" to project_version,
            "loader_version" to loader_version,
            "minecraft_required" to project.property("minecraft_required")?.toString()
        )
        inputs.properties(map)

        from(scourPackages) {
            rename { "asm/$it" }
        }

        filesMatching("fabric.mod.json") {
            expand(map)
        }
    }
    jar {
        // FIXME: Loom is broken. This is the only viable workaround without hacking it to pieces.
        //  As such, this JAR will never work due to the transformer active inside of it and should be
        //  considered broken by any and all trying to put this JAR into their dev environment.
        exclude("asm/")
    }
    remapJar {
        dependsOn(":utilities:jar")
        // FIXME: Loom is broken. This is not the proper method, this is only done as remapJar
        //  does not allow intercepting any files that exists in the development JAR. This does
        //  mean that by nature the development JAR is broken.
        from(scourPackages) {
            // force insertion in case of duplication
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            rename { "asm/$it" }
            filter(::transformer)
        }
        from(sourceSets.main.get().resources.srcDir("asm")) {
            filter(::transformer)
        }
    }
    withType<Jar> {
        from("LICENSE")
    }
    val publishToModrinth = register<TaskModrinthUpload>("publishToModrinth") {
        group = "publishing"
        dependsOn(remapJar)
        token = System.getenv("MODRINTH_TOKEN")
        projectId = modrinth_id
        versionNumber = version.toString()
        versionType = System.getenv("RELEASE_OVERRIDE")?.let(VersionType::valueOf) ?: when {
            "alpha" in project_version -> VersionType.ALPHA
            !isRelease || '-' in project_version -> VersionType.BETA
            else -> VersionType.RELEASE
        }
        val ref = System.getenv("GITHUB_REF")
        changelog = System.getenv("CHANGELOG") ?: if (ref.startsWith("refs/tags/")) "You may view the changelog at https://github.com/the-glitch-network/plymouth-fabric/releases/tag/${com.google.common.net.UrlEscapers.urlFragmentEscaper().escape(ref.substring(10))}"
        else "No changelog is available. Perhaps poke at https://github.com/the-glitch-network/plymouth-fabric for a changelog?"
        uploadFile = remapJar.get()
        addGameVersion(minecraft_version)
        addLoader("fabric")
    }
    publish {
        dependsOn(publishToModrinth)
    }
}