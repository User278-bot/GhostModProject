plugins {
    id("java-library")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenCentral()

}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    api("com.ghost:ghost-common:dev")
    api("org.java-websocket:Java-WebSocket:${property("websocket_version")}")
    implementation("org.slf4j:slf4j-api:${property("slf4j_version")}")
}

group = "com.ghost"
version = "1.0-SNAPSHOT"
