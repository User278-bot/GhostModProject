plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // ghost-api の build.gradle.kts で protobuf プラグインを使うために必要
    implementation("com.google.protobuf:protobuf-gradle-plugin:0.10.0")
}
