plugins {
    id("java")
}

val gitVersion: String? = System.getenv("GITHUB_REF")?.let { ref ->
    if (ref.startsWith("refs/tags/")) ref.substringAfter("refs/tags/") else null
} ?: try {
    val process = ProcessBuilder("git", "describe", "--tags", "--exact-match")
        .redirectErrorStream(true)
        .start()
    process.waitFor()
    if (process.exitValue() == 0) {
        process.inputStream.bufferedReader().readText().trim().takeIf { it.isNotEmpty() }
    } else {
        null
    }
} catch (e: Exception) {
    null
}

allprojects {
    group = "com.ghost"
    version = gitVersion ?: "dev"
}


