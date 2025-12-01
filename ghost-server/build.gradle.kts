import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.JavaExec
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("ghost-java-conventions")
    id("com.gradleup.shadow") version "9.2.2"
    application
}

dependencies {
    implementation(project(":ghost-common"))
    implementation(project(":ghost-network"))
    implementation("org.java-websocket:Java-WebSocket:${rootProject.property("websocket_version")}")
    implementation("commons-cli:commons-cli:1.6.0")
    runtimeOnly("org.slf4j:slf4j-simple:${rootProject.property("slf4j_version")}")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.ghost.server.GhostModServer")
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