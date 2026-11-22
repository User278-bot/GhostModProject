---
trigger: always_on
---

Minecraft や依存ライブラリの内部実装・クラス構造を調査する場合は、特別な理由がない限り `javap` などのコマンドラインツールを使用せず、IDE の機能（`jetbrains_air_ide` の `search_symbol` および `read_file`）を優先して使用してください。

- **クラス・シンボルの探索**: `search_symbol` に `--include_external true` を指定して、対象バージョン（`.gradle/loom-cache/minecraftMaven/...` 等）の `.jar!/.../*.class` パスを取得する。
- **逆コンパイル・実装確認**: `search_symbol` で取得したプロジェクト／ライブラリルート配下のパスを IDE の `read_file` に渡し、IntelliJ IDEA のデコンパイラ経由で Java ソースとして参照する（グローバルキャッシュ `~/.gradle/caches/...` の直接指定はルート外エラーとなるため避ける）。
- **コマンドラインでの確認が必要な場合**: IDE 機能で参照できない場合（例: プロジェクト外ファイルの確認や特殊なバイトコード検証など）に限定して使用する。
