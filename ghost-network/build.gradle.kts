plugins {
    id("java-library")
}

dependencies {
    api(project(":ghost-common"))
    api("org.java-websocket:Java-WebSocket:${property("websocket_version")}")
    implementation("org.slf4j:slf4j-api:${property("slf4j_version")}")
}

group = "com.ghost"
version = "1.0-SNAPSHOT"
