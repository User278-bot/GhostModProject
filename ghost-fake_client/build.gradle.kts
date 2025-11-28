import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ghost-common"))
    implementation(project(":ghost-network"))
    implementation("commons-cli:commons-cli:1.6.0")
    runtimeOnly("org.slf4j:slf4j-simple:${rootProject.property("slf4j_version")}")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("com.ghost.fake_client.FakeClientMain")
}

tasks.withType<ShadowJar> {
    // 出力されるJARファイル名のベース部分を指定
    archiveBaseName.set("GhostFakeClient")
    // バージョン番号を指定
    archiveVersion.set("${project.version}")
    // ファイル名に `-all` や `-shadow` といった接尾辞が付かないようにする
    archiveClassifier.set("")

    // 生成されるJARのマニフェストファイルに、メインクラスを指定する
    // これにより `java -jar` コマンドで直接実行できるようになる
    manifest {
        attributes["Main-Class"] = "com.ghost.fake_client.FakeClientMain"
    }
}