import java.net.URI

plugins {
    id("java")
}

group = "com.ghost"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        maven {
            name = "Fabric"
            url = URI("https://maven.fabricmc.net/")
        }
    }
}

subprojects {
    apply(plugin = "java")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}