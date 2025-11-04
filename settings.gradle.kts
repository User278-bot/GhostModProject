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

rootProject.name = "GhostModProject_2"

include("ghost-mod")
include("ghost-server")
include("ghost-common")
include("ghost-fake_client")
