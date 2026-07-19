# Ghost Mod Project

AI Translated: English
[README_en.md](README_en.md)

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
- Minecraft 1.20.6
- Minecraft 1.21.4
- Minecraft 1.21.11

## 成果物のダウンロード
以下のリンクから成果物をダウンロードできます。表示されている番号が最新バージョンです。リンク先が正しくない場合は、[リリース](https://github.com/User278-bot/GhostModProject/releases)から各成果物の最新バージョンをダウンロードしてください。

[![Mod Version](https://img.shields.io/github/v/release/User278-bot/GhostModProject?include_prereleases&filter=mod/v*&label=Mod&color=blue)](https://github.com/User278-bot/GhostModProject/releases/tag/mod/v1.3.1)
[![Server Version](https://img.shields.io/github/v/release/User278-bot/GhostModProject?include_prereleases&filter=server/v*&label=Server&color=green)](https://github.com/User278-bot/GhostModProject/releases/tag/server/v1.3.1)
[![FakeClient Version](https://img.shields.io/github/v/release/User278-bot/GhostModProject?include_prereleases&filter=client/v*&label=FakeClient&color=orange)](https://github.com/User278-bot/GhostModProject/releases/tag/client/v1.3.0)

## 導入方法

### 1. Modの導入 (クライアント)
1.  `ghostmod-<version>+<mc_version>.jar` を Minecraft の `mods` フォルダに配置してください。
2.  以下のModもMinecraftバージョンに合わせて導入してください。
    - **必須**
        - [Fabric API](https://modrinth.com/mod/fabric-api)
        - [Cloth Config API](https://modrinth.com/mod/cloth-config)
    - **オプション**
        - [Mod Menu](https://modrinth.com/mod/modmenu)

### 2. サーバーの起動
`GhostModServer-<version>.jar` を実行して同期サーバーを立ち上げます。

```bash
java -jar GhostModServer-<vesion>.jar
```
デフォルトではポート `8887` で待機します。ポートを変更したい場合は `--port` オプションを使用してください。

```bash
java -jar GhostModServer-<vesion>.jar --port 9000 --password <password> --config <filename> --view-distance <distance> --rate-limit <rate>
```

> [!IMPORTANT]
> LAN以外（離れた友達と遊ぶ場合など）では、サーバーを外部に公開する必要があります。
>
> - **ポート開放**: ルーターの設定でポート `8887` (または指定したポート) を解放してください。セキュリティ設定は各自の責任で行ってください。
> - **ポート開放が難しい場合**: 以下のサービスを利用することで、ポート開放なしで接続可能です。
>     - [Fast Server](https://fss.zpw.jp/) の **ConnectXross** (推奨・動作確認済み): サーバー側のみ設定が必要で、接続側（友達）は設定不要なため最も手軽です。
>     - [playit.gg](https://playit.gg/): ConnectXrossと同様、接続側の設定が不要なトンネリングサービスです。
>     - [Hamachi](https://www.vpn.net/): 仮想LAN構築サービス。全員がソフトをインストールして同じネットワークに入る必要があります。

## 使い方
1.  サーバーを起動しておきます（ローカルで遊ぶ場合は自分のPCで、離れた友達と遊ぶ場合はVPSなどで）。
2.  Modを導入した状態で Minecraft を起動します。
3.  Mod Menu またはショートカットキー（デフォルトではＧキー）から **Ghost Mod** の設定画面を開きます。
4.  サーバーのアドレス・ポート、パスワードを入力して保存します。

![Minecraft_ 1 19 2 2025_12_07 15_25_20](https://github.com/user-attachments/assets/6b8b27db-694b-4c3d-bd01-b5a3b891d6a7)
![Minecraft_ 1 20 1 2025_12_21 8_51_29](https://github.com/user-attachments/assets/00ecd29f-e7f0-49da-a643-8b6072ca7cdf)

5.  シングルプレイのワールドに入ると、**自動的に**サーバーへ接続されます。
6.  同じサーバーに接続している他のプレイヤーが、ワールド内にゴーストとして表示されます。

> [!NOTE]
> シングルプレイワールドに入っている間は接続、切断ボタンが有効化されます。必要に応じて使用してください。

## 開発者向け情報
### ビルド方法
プロジェクトのルートディレクトリで以下のコマンドを実行してください。

```bash
./gradlew build
```

### 成果物の出力先

ビルドが成功すると、以下の場所に成果物 (JARファイル) が生成されます。

#### Mod (ghost-mod)
バージョンごとに以下のディレクトリに出力されます。
- `ghost-mod/versions/1.19.2/build/libs/ghostmod-<vesion>+1.19.2.jar`
- `ghost-mod/versions/1.20.1/build/libs/ghostmod-<vesion>+1.20.1.jar`
など

#### Server (ghost-server)
- `ghost-server/build/libs/GhostModServer-<version>.jar`
※ `GhostModServer-*.jar` (ファイルサイズが大きい方) が実行可能なサーバーアプリケーションです。

### FakeClient
負荷テストや同期確認のために、ダミーのプレイヤーデータを送信する `FakeClient` が用意されています。

```bash
./gradlew :ghost-fake_client:run --args="--uuid <UUID> --uri <URI> --password <password>"
```

### デバッグ環境の起動
PowerShellスクリプトを使用して、サーバー・クライアント・FakeClientを一括で起動できます。

```powershell
# デバッグ環境の起動
.\start-debug.ps1

# デバッグ環境の終了
.\stop-debug.ps1
```

## ライセンス
本プロジェクトは [MIT License](LICENSE) の下で公開されています。
AI生成コードを含むため、無保証（AS IS）であることをご留意ください。
