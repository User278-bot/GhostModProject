pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = java.net.URI("https://maven.fabricmc.net/")
        }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.11"
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
        versions("1.19.2","1.20.1")
        vcsVersion = "1.19.2"
    }
}

rootProject.name = "ghost-project"

include("ghost-mod")
include("ghost-server")
include("ghost-common")
include("ghost-fake_client")
include("ghost-network")

