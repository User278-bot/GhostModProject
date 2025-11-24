pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = java.net.URI("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "ghost-project"

includeBuild("ghost-mod")
//include("ghost-server")
//includeBuild("ghost-common")
//include("ghost-fake_client")
//includeBuild("ghost-network")
