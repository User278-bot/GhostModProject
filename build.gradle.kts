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
    apply(plugin = "java")
    dependencies {

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

