pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
    id("dev.kikugie.loom-back-compat") version "0.4"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// リポジトリの一元管理
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    
    // Fabric Loomとの互換性のため、プロジェクトレベルのリポジトリを優先
    // ghost-modはFabric Loom固有のリポジトリ設定が必要なため
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
}

stonecutter {
    create("ghost-mod") {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        versions("1.19.2", "1.20.1", "1.20.6", "1.21.4", "1.21.11")
        version("26.1.x", "26.1.2")
        version("26.2.x", "26.2")
        vcsVersion = "1.19.2"
    }
}

rootProject.name = "ghost-project"

include("ghost-mod")
include("ghost-server")
include("ghost-api")
include("ghost-fake_client")
include("ghost-network")

