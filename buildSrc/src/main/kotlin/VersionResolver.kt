object VersionResolver {
    /**
     * 指定されたプレフィックスを持つ最新のGitタグからバージョンを解決する
     * @param prefix タグのプレフィックス (例: "mod/v", "server/v")
     * @return バージョン文字列 (プレフィックスを除いた部分) または null
     */
    fun resolveVersionFromTag(prefix: String): String? {
        // CI環境: GITHUB_REFからバージョンを取得
        val ciVersion = System.getenv("GITHUB_REF")?.let { ref ->
            if (ref.startsWith("refs/tags/$prefix")) {
                ref.substringAfter("refs/tags/$prefix")
            } else null
        }
        
        if (ciVersion != null) return ciVersion
        
        // ローカル環境: gitコマンドで最新のタグを取得
        return try {
            val process = ProcessBuilder("git", "tag", "-l", "$prefix*", "--sort=-version:refname")
                .redirectErrorStream(true)
                .start()
            process.waitFor()
            if (process.exitValue() == 0) {
                process.inputStream.bufferedReader().readLines()
                    .firstOrNull()
                    ?.substringAfter(prefix)
                    ?.takeIf { it.isNotEmpty() }
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
