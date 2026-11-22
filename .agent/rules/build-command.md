---
trigger: always_on
---

全体のビルド及び全バージョンのMod・テスト・成果物（JAR / ShadowJar / RemapJar）を確認する場合は、特別な理由がない限りコマンドラインから直接実行せず、IDE の実行構成 `ghost-project [build]`（`execute_run_configuration`）を優先して実行してください。

- **全体フルビルド（推奨）**: IDE の実行構成 `ghost-project [build]` を実行（`./gradlew build` と同等）。
- **コンパイルエラーのみの簡易確認**: IDE の `build_project` 機能を使用可能。
- **コマンドラインで実行する必要がある場合**: `./gradlew build` を使用してください（`chiseledBuild` は使用できません）。
