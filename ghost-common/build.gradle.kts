plugins {
    id("ghost-java-conventions")
}

dependencies {
    api(libs.gson)
    api(libs.slf4j.api)

    compileOnly(libs.jetbrains.annotations)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
}