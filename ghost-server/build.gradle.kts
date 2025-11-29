import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.JavaExec
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
    application
}

version=project.property("ghost-server.version")as String
group=project.property("ghost-server.group")as String

repositories {
    mavenCentral()
}

dependencies {
    implementation(("com.ghost:ghost-network:${property("ghost-network.version")}"))
    implementation("org.java-websocket:Java-WebSocket:${property("websocket.version")}")
    implementation("commons-cli:commons-cli:${property("commons-cli.version")}")
    runtimeOnly("org.slf4j:slf4j-simple:${property("slf4j.version")}")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.ghost.server.GhostModServer")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.named<JavaExec>("run") {
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.withType<ShadowJar> {
    // 出力されるJARファイル名のベース部分を指定
    archiveBaseName.set("GhostModServer")
    // バージョン番号を指定
    archiveVersion.set("${project.version}")
    // ファイル名に `-all` や `-shadow` といった接尾辞が付かないようにする
    archiveClassifier.set("")

    // 生成されるJARのマニフェストファイルに、メインクラスを指定する
    // これにより `java -jar` コマンドで直接実行できるようになる
    manifest {
        attributes["Main-Class"] = "com.ghost.server.GhostModServer"
    }
}