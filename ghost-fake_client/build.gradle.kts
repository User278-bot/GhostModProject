import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("ghost-java-conventions")
    id("com.gradleup.shadow") version "9.2.2"
    application
}

val clientVersion = VersionResolver.resolveVersionFromTag("client/v") ?: "dev"
version = clientVersion

dependencies {
    implementation(project(":ghost-network"))
    implementation(libs.commons.cli)
    runtimeOnly(libs.slf4j.simple)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.bundles.test)
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