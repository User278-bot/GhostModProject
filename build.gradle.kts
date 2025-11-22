plugins {
    id("java")
}

import java.io.ByteArrayOutputStream

fun getGitVersion(): String {
    return try {
        val stdout = ByteArrayOutputStream()
        val result = project.exec {
            commandLine("git", "describe", "--tags", "--exact-match")
            standardOutput = stdout
            isIgnoreExitValue = true
        }
        if (result.exitValue == 0) {
            // タグと完全一致する場合 (例: v1.0.0 -> 1.0.0)
            stdout.toString().trim().removePrefix("v")
        } else {
            // タグと一致しない場合 (開発中) -> 固定バージョン
            "dev-SNAPSHOT"
        }
    } catch (e: Exception) {
        "dev-SNAPSHOT"
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

