plugins {
    id("java-library")
}

dependencies {
    api("com.google.code.gson:gson:${property("gson_version")}")
    api("org.slf4j:slf4j-api:${property("slf4j_version")}")
}