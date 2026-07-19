import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.JavaExec
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("ghost-java-conventions")
    id("com.gradleup.shadow") version "9.2.2"
    application
}

val serverVersion = VersionResolver.resolveVersionFromTag("server/v") ?: "dev"
version = serverVersion

dependencies {
    implementation(project(":ghost-network"))
    implementation(libs.websocket)
    implementation(libs.commons.cli)
    runtimeOnly(libs.slf4j.simple)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
}

application {
    mainClass.set("com.ghost.server.GhostModServer")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
    doFirst {
        // IntelliJがデバッグ実行時に差し込むJDWPエージェントが含まれているかチェック
        val isDebugExecute = jvmArgs?.any { it.contains("-agentlib:jdwp") } == true

        if (isDebugExecute) {
            println("🐛 デバッグモードを検知: slf4j-simple のログレベルを DEBUG に切り替えます")
            systemProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug")
        }
    }
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