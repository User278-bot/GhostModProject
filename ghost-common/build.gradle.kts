plugins {
    id("java-library")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

group=project.property("common_group")as String
version=project.property("common_version")as String

repositories {
    mavenCentral()
}

dependencies {
    api("com.google.code.gson:gson:${property("gson_version")}")
    api("org.slf4j:slf4j-api:${property("slf4j_version")}")

    compileOnly("org.jetbrains:annotations:26.0.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}