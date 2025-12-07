# Ghost Mod Project

**Ghost Mod** は、一人用の配布マップなどを擬似的にマルチプレイ化するための Minecraft Mod およびサーバーソフトウェアです。

各プレイヤーの位置情報をリアルタイムで同期し、他のプレイヤーのクライアント上に「ゴースト（幻影）」として表示します。
このゴーストはクライアントサイドのみに存在するエンティティであり、サーバー側のロジック（コマンドブロックやダメージ判定など）には一切干渉しません。これにより、配布マップのギミックを壊すことなく、友達と一緒に探索を楽しむことができます。

> [!NOTE]
> 本プロジェクトは **Antigravity** および **Google AI Studio** を活用して開発されており、コードの大部分はAIによって生成されています。

## プロジェクト構成
本プロジェクトは以下のモジュールで構成されています。

- **ghost-mod**: Minecraft Mod 本体 (Fabric)。Stonecutter を使用して複数バージョンに対応しています。
- **ghost-server**: 同期用の中継サーバー (Java Application)。
- **ghost-fake_client**: 負荷テスト・デバッグ用のダミーライアント。
- **ghost-api**: 共通のパケット定義やデータ構造。

## 動作環境
- **Minecraft Version**: [対応バージョン](#対応バージョン)を参照
- **Mod Loader**: Fabric
- **Java Version**: Java 21

## 対応バージョン
- Minecraft 1.19.2
- Minecraft 1.20.1

## 成果物のダウンロード
以下のリンクから最新版がダウンロードできます。

[![Mod Version](https://img.shields.io/github/v/release/User278-bot/GhostModProject?include_prereleases&filter=mod/v*&label=Mod&color=blue)](https://github.com/User278-bot/GhostModProject/releases?q=[mod])
[![Server Version](https://img.shields.io/github/v/release/User278-bot/GhostModProject?include_prereleases&filter=server/v*&label=Server&color=green)](https://github.com/User278-bot/GhostModProject/releases?q=[server])
[![FakeClient Version](https://img.shields.io/github/v/release/User278-bot/GhostModProject?include_prereleases&filter=client/v*&label=FakeClient&color=orange)](https://github.com/User278-bot/GhostModProject/releases?q=[fakeclient])

## ビルド方法
プロジェクトのルートディレクトリで以下のコマンドを実行してください。

```bash
./gradlew build
```

### 成果物の出力先

ビルドが成功すると、以下の場所に成果物 (JARファイル) が生成されます。

#### Mod (ghost-mod)
バージョンごとに以下のディレクトリに出力されます。
- `ghost-mod/versions/1.19.2/build/libs/ghostmod-0.1.0+1.19.2.jar`
- `ghost-mod/versions/1.20.1/build/libs/ghostmod-0.1.0+1.20.1.jar`

#### Server (ghost-server)
- `ghost-server/build/libs/GhostModServer-0.1.0.jar`
※ `GhostModServer-*.jar` (ファイルサイズが大きい方) が実行可能なサーバーアプリケーションです。

## 導入方法

### 1. Modの導入 (クライアント)
1.  `ghostmod-<version>+<mc_version>.jar` を Minecraft の `mods` フォルダに配置してください。
2.  以下の前提 Mod も合わせて導入してください。
    - [Fabric API](https://modrinth.com/mod/fabric-api)
    - [Mod Menu](https://modrinth.com/mod/modmenu)
    - [Cloth Config API](https://modrinth.com/mod/cloth-config)

### 2. サーバーの起動
`GhostModServer-<version>.jar` を実行して同期サーバーを立ち上げます。

```bash
java -jar GhostModServer-0.1.0.jar
```
デフォルトではポート `8887` で待機します。ポートを変更したい場合は `--port` オプションを使用してください。

```bash
java -jar GhostModServer-0.1.0.jar --port 9000
```

## 使い方
1.  サーバーを起動しておきます（ローカルで遊ぶ場合は自分のPCで、離れた友達と遊ぶ場合はVPSなどで）。
2.  Modを導入した状態で Minecraft を起動します。
3.  Mod Menu から **Ghost Mod** の設定画面を開き、サーバーのアドレスとポートを入力して保存します。

![Minecraft_ 1 19 2 2025_12_07 15_25_20](https://github.com/user-attachments/assets/5fbd055b-2497-4253-860d-ee5a92ee8c38)
![Minecraft_ 1 19 2 2025_12_07 15_25_29](https://github.com/user-attachments/assets/e9807190-bf03-4306-a0f0-47313b511237)

4.  シングルプレイのワールドに入ると、自動的にサーバーに接続されます。
5.  同じサーバーに接続している他のプレイヤーが、ワールド内にゴーストとして表示されます。

> [!NOTE]
> シングルプレイワールドに入っている間は接続、切断ボタンが有効化されます。必要に応じて使用してください。

## 開発者向け情報

### デバッグ環境の起動
PowerShellスクリプトを使用して、サーバー・クライアント・FakeClientを一括で起動できます。

```powershell
# デバッグ環境の起動
.\start-debug.ps1

# デバッグ環境の終了
.\stop-debug.ps1
```

### FakeClient
負荷テストや同期確認のために、ダミーのプレイヤーデータを送信する `FakeClient` が用意されています。

```bash
./gradlew :ghost-fake_client:run --args="--uuid <UUID> --uri <URI>"
```

## ライセンス
本プロジェクトは [MIT License](LICENSE) の下で公開されています。
AI生成コードを含むため、無保証（AS IS）であることをご留意ください。
