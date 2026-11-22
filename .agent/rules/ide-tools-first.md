---
trigger: always_on
---

ファイルの読み書き・検索・シンボル調査・Git 状態確認・ビルド・リファクタリングなどの作業は、原則としてすべて IDE（`jetbrains_air_ide` MCP）やエージェントの専用ツールで実行してください。シェルコマンドの実行（`run_command` / `execute_terminal_command`）は、現在の IDE・MCP・専用ツール機能ではどうしても実現できない場合にのみ限定してください。

- **ファイル・ディレクトリ確認**: `list_directory_tree` / `search_file` / `view_file` / IDE の `read_file` を使用する（`Get-ChildItem` / `ls` / `Get-Content` 等は使わない）。
- **コード・シンボル検索・呼び出し解析**: `search_text` / `search_regex` / `search_symbol` / `get_symbol_info` / `analyze_calls` を使用する（`git grep` / `Select-String` 等は使わない）。
- **外部ライブラリ・Minecraft 内部実装の調査**: `search_symbol`（`--include_external true`）と IDE の `read_file` を使用する（`javap` 等は使わない）。
- **Git 状態確認**: `git_status` を優先使用する（差分詳細の確認など専用ツールが存在しない操作のみ必要最小限でコマンドを使用する）。
- **静的解析・整形・リファクタリング・ビルド**: `get_file_problems` / `lint_files` / `reformat_file` / `rename_refactoring` / `build_project` / `execute_run_configuration` を使用する。
- **コマンド実行が許容される例外**: IDE・MCP・専用ツールでは取得・実行できない操作（例: `git diff` / `git log` の確認、ワークスペース外ログの参照、Gradle デーモン停止などの障害復旧）に限る。
