plugins {
    id("java-library")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    compileOnly("org.jetbrains:annotations:26.0.2")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    api(project(":ghost-common"))
    api("org.java-websocket:Java-WebSocket:${rootProject.property("websocket_version")}")
    implementation("org.slf4j:slf4j-api:${rootProject.property("slf4j_version")}")
}
