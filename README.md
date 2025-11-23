# Ghost Mod Project

**Ghost Mod** は、一人用の配布マップなどを擬似的にマルチプレイ化するための Minecraft Mod およびサーバーソフトウェアです。

各プレイヤーの位置情報をリアルタイムで同期し、他のプレイヤーのクライアント上に「ゴースト（幻影）」として表示します。
このゴーストはクライアントサイドのみに存在するエンティティであり、サーバー側のロジック（コマンドブロックやダメージ判定など）には一切干渉しません。これにより、配布マップのギミックを壊すことなく、友達と一緒に探索を楽しむことができます。

> [!NOTE]
> 本プロジェクトは **Antigravity** および **Google AI Studio** を活用して開発されており、コードの大部分はAIによって生成されています。

## 動作環境
- **Minecraft Version**: 1.19.2
- **Mod Loader**: Fabric
- **Java Version**: Java 21

## ビルド方法
プロジェクトのルートディレクトリで以下のコマンドを実行してください。
成果物（JARファイル）は各モジュールの `build/libs` ディレクトリに出力されます。

```bash
./gradlew build
```

## 導入方法

### 1. Modの導入 (クライアント)
`ghost-mod/build/libs/ghost-mod-<version>.jar` を Minecraft の `mods` フォルダに配置してください。

**必須依存 Mod:**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Mod Menu](https://modrinth.com/mod/modmenu)
- [Cloth Config API](https://modrinth.com/mod/cloth-config)

### 2. サーバーの起動
`ghost-server/build/libs/GhostModServer-<version>.jar` を実行して同期サーバーを立ち上げます。`--port <port>` でポートを指定できます（省略可）。

```bash
java -jar GhostModServer-<version>.jar
```
デフォルトではポート `8887` で待機します。

## 使い方
1.  Modを導入した状態で Minecraft を起動します。
2.  Mod Menu から **Ghost Mod** の設定画面を開き、サーバーのアドレス（例: `ws://localhost:8887`）を入力します。
3.  シングルプレイのワールドに入ると、自動的にサーバーに接続されます。
4.  同じサーバーに接続している他のプレイヤーが、ワールド内にゴーストとして表示されます。

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
