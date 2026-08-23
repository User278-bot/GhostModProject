plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

val baseVersion = VersionResolver.resolveVersionFromTag("mod/v") ?: "dev"

ext["mod.version"] = baseVersion
version = "$baseVersion+${sc.current.version}"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.shedaniel.me/")

    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
}

dependencies {
    /**
     * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
     * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
     */
    // fapi function removed
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")


    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${project.property("cloth_config_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
        exclude(group = "net.fabricmc", module = "fabric-loader")
    }
    // ModMenuはオプショナル依存 - コンパイル時のみ使用
    modCompileOnly("maven.modrinth:modmenu:${project.property("mod_menu_version")}") {
        exclude(group = "net.fabricmc.fabric-api")
        exclude(group = "net.fabricmc", module = "fabric-loader")
    }
    implementation(project(":ghost-network"))
    include(project(":ghost-network"))
    include(project(":ghost-api"))

    // 外部ライブラリも明示的にincludeが必要（Loomのincludeは推移的ではない）
    // ghost-networkが依存しているWebSocketライブラリを含める
    include(libs.websocket)
    // ghost-apiが依存しているProtobufライブラリを含める
    include(libs.protobuf.java)
}

loom {
    splitEnvironmentSourceSets()

    fabricModJsonPath =
        (project.parent ?: project).file("src/main/resources/fabric.mod.json") // Useful for interface injection

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    mods {
        register(project.property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets["client"])
        }
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true

        jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDirectory = file("./run")  // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep")
        )

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

// ModrinthとCurseForgeにビルドを公開
publishMods {
    file = loomx.modJar.flatMap { it.archiveFile }
    additionalFiles.from(loomx.modSourcesJar.flatMap { it.archiveFile })
    displayName = "${property("mod.name")} ${property("mod.version")} for ${property("mod.mc_title")}"
    version = property("mod.version") as String
    changelog = (project.parent ?: project).file("CHANGELOG.md").readText()
    val releaseTypeStr = (project.findProperty("release_type") as? String)?.uppercase() ?: "STABLE"
    type = when (releaseTypeStr) {
        "ALPHA" -> ALPHA
        "BETA" -> BETA
        else -> STABLE
    }
    modLoaders.add("fabric")

    // トークンが未設定または空の場合はdryRunモード（実際のアップロードは行わない）
    dryRun = providers.environmentVariable("MODRINTH_TOKEN").orNull.isNullOrBlank()
            || providers.environmentVariable("CURSEFORGE_TOKEN").orNull.isNullOrBlank()

    modrinth {
        projectId = property("publish.modrinth") as String
        accessToken = providers.environmentVariable("MODRINTH_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
        requires {
            slug = "cloth-config"
        }
        optional {
            slug = "modmenu"
        }
    }

    curseforge {
        projectId = property("publish.curseforge") as String
        accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
        minecraftVersions.addAll(property("mod.mc_targets").toString().split(' '))
        requires {
            slug = "fabric-api"
        }
        requires {
            slug = "cloth-config"
        }
        optional {
            slug = "modmenu"
        }
        clientRequired = true
        serverRequired = false
    }

    // discord {
    //     webhookUrl = providers.environmentVariable("DISCORD_WEBHOOK")
    // }
}
/*
// Publishes builds to a maven repository under `com.example:template:0.1.0+mc`
publishing {
    repositories {
        maven("https://maven.example.com/releases") {
            name = "myMaven"
            // To authenticate, create `myMavenUsername` and `myMavenPassword` properties in your Gradle home properties.
            // See https://stonecutter.kikugie.dev/wiki/tips/properties#defining-properties
            credentials(PasswordCredentials::class.java)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "${property("mod.group")}.${property("mod.id")}"
            artifactId = property("mod.id") as String
            version = project.version

            from(components["java"])
        }
    }
}
 */
