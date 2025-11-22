plugins {
    id("java")
}

import java.io.ByteArrayOutputStream

fun getGitVersion(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        val result = project.exec {
            commandLine("git", "describe", "--tags")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        if (result.exitValue == 0) {
            stdout.toString().trim().removePrefix("v")
        } else {
            project.findProperty("mod_version")?.toString() ?: "0.0.0-SNAPSHOT"
        }
    } catch (e: Exception) {
        project.findProperty("mod_version")?.toString() ?: "0.0.0-SNAPSHOT"
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
    group = "com.ghost"
    version = getGitVersion()
}

subprojects {
    apply(plugin = "java")
    dependencies {
        compileOnly("org.jetbrains:annotations:26.0.2")
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
    tasks.test {
        useJUnitPlatform()
    }
}

