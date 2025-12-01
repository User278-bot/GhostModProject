plugins {
    id("ghost-java-conventions")
}

dependencies {
    api(project(":ghost-common"))
    api(libs.websocket)
    implementation(libs.slf4j.api)
    
    compileOnly(libs.jetbrains.annotations)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
}
