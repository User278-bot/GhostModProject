import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
    application
}

group = "com.ghost"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ghost-common"))
    implementation(project(":ghost-network"))
    runtimeOnly("org.slf4j:slf4j-simple:${property("slf4j_version")}")
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
    archiveBaseName.set("FakeClientMain")
    // バージョン番号を指定
    archiveVersion.set("1.0.0")
    // ファイル名に `-all` や `-shadow` といった接尾辞が付かないようにする
    archiveClassifier.set("")

    // 生成されるJARのマニフェストファイルに、メインクラスを指定する
    // これにより `java -jar` コマンドで直接実行できるようになる
    manifest {
        attributes["Main-Class"] = "com.ghost.FakeClientMain"
    }
}