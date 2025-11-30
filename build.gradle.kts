plugins {
    id("java")
}

allprojects {
    repositories {
        mavenCentral()
    }
    group = "com.ghost"
    version = "dev"
}

subprojects {
    // StoneCutterのバージョン別プロジェクトも除外
    if (!path.startsWith(":ghost-mod")) {
        apply(plugin = "java")
        
        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
            }
        }
        tasks.test {
            useJUnitPlatform()
        }
    }
}


