
plugins {
    id("ghost-java-conventions")
    id("com.google.protobuf")
}

dependencies {
    api(libs.gson)
    api(libs.slf4j.api)
    // protobuf-java: PlayerData等のシリアライズに使用
    api(libs.protobuf.java)

    compileOnly(libs.jetbrains.annotations)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
}

protobuf {
    protoc {
        // [versions] の protobuf からバージョン文字列を取得
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.asProvider().get()}"
    }
}