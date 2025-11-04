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

include("ghost-mod")
include("ghost-server")
include("ghost-common")
include("ghost-fake_client")
include("ghost-network")
