# Ghost Mod

**Explore adventure maps with friends — without breaking the map!**

Ghost Mod is a client-side Minecraft mod that lets you play single-player adventure maps together with friends by displaying their positions as "ghosts" in your world.

> ⚠️ **IMPORTANT: Server Software Required**
>
> This mod requires a separate **Ghost Mod Server** to function. The server software is **not included in this mod** and must be downloaded separately from our [GitHub Releases](https://github.com/User278-bot/GhostModProject/releases) page. One person in your group needs to run the server for everyone to connect.
>
> Please use matching **major.minor versions (`X.Y` in `vX.Y.z`)** for both the **Mod** and **Ghost Mod Server** (e.g., `v1.7.x` with `v1.7.x`). Different patch versions (`.z`) remain compatible.
>
> For detailed server setup and usage instructions, please refer to the [GitHub README](https://github.com/User278-bot/GhostModProject#readme).

## ✨ What It Does

- **Real-time player sync**: See your friends' positions, poses (sneaking, swimming, etc.), equipment, and arm swings in real time
- **Client-side only ghosts**: Other players appear as transparent "ghost" entities with their player skins
- **No map interference**: Ghosts don't trigger command blocks, take damage, or affect any server-side logic
- **Perfect for adventure maps**: Play through puzzles and stories together without breaking anything

## 🎮 How It Works

1. One person runs the **Ghost Mod Server** (a separate Java 17+ application)
2. Everyone installs this mod and connects to the server
3. Enter any single-player world — you'll automatically connect
4. See your friends as ghosts while you each play your own copy of the map!

## 📦 Requirements

**Required:**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config API](https://modrinth.com/mod/cloth-config)

**Optional:**
- [Mod Menu](https://modrinth.com/mod/modmenu) — for easy settings access

## ⚙️ Configuration

Open the settings screen via:
- **Mod Menu** → Ghost Mod
- **Shortcut key** (default: G)

Enter your server's address, port, and password to connect.

## 🖥️ Server Software

The Ghost Mod Server (Java 17+) is available separately on our [GitHub Releases](https://github.com/User278-bot/GhostModProject/releases) page.

```bash
java -jar GhostModServer-<version>.jar
```

Default port: `8887`

## 📖 More Information

For detailed setup instructions, server configuration options, and developer documentation, please visit our [GitHub Repository](https://github.com/User278-bot/GhostModProject).

## ⚠️ Note

This project was developed with assistance from AI tools (Antigravity & Google AI Studio). The code is provided "AS IS" under the MIT License.

---

# Ghost Mod（日本語）

**配布マップを友達と一緒にプレイ — マップのギミックを壊さずに！**

Ghost Modは、シングルプレイ用の配布マップを友達と一緒にプレイできるようにするクライアントサイドのMinecraft Modです。他のプレイヤーの位置を「ゴースト（幻影）」としてあなたのワールドに表示します。

> ⚠️ **重要: サーバーソフトウェアが必要です**
>
> このModを使用するには、別途 **Ghost Mod Server** が必要です。サーバーソフトウェアは**このModには含まれておらず**、[GitHubリリース](https://github.com/User278-bot/GhostModProject/releases)ページから別途ダウンロードする必要があります。グループの誰か一人がサーバーを起動し、全員が接続します。
>
> **Mod と Ghost Mod Server はメジャー・マイナーバージョン（`vX.Y.z` の `X.Y` 部分）を揃えて使用してください**（例: `v1.7.x` 同士）。パッチバージョン（`.z`）の違いには互換性があります。
>
> サーバーのセットアップと使用方法の詳細は、[GitHub README](https://github.com/User278-bot/GhostModProject#readme)をご参照ください。

## ✨ 機能

- **リアルタイム同期**: 同じ配布マップ内の友達の位置・姿勢（スニークや泳ぎ等）・装備・腕振りなどをリアルタイムに同期
- **クライアントサイドのみのゴースト**: 他プレイヤーは本人のスキンを反映した半透明の「ゴースト」エンティティとして表示
- **マップへの干渉なし**: ゴーストはコマンドブロックを作動させたり、ダメージを受けたりしない
- **配布マップに最適**: パズルやストーリーを一緒に楽しめる

## 🎮 使い方

1. 誰か一人が **Ghost Mod Server**（別途 Java 17 以上のアプリケーション）を起動
2. 全員がこのModをインストールし、サーバーに接続
3. シングルプレイワールドに入ると自動的に接続
4. 各自が同じマップをプレイしながら、友達をゴーストとして見れる！

## 📦 必要なMod

**必須:**
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config API](https://modrinth.com/mod/cloth-config)

**オプション:**
- [Mod Menu](https://modrinth.com/mod/modmenu) — 設定画面に簡単にアクセス

## ⚙️ 設定

設定画面の開き方:
- **Mod Menu** → Ghost Mod
- **ショートカットキー**（デフォルト: Gキー）

サーバーのアドレス、ポート、パスワードを入力して接続してください。

## 🖥️ サーバーソフトウェア

Ghost Mod Server（Java 17 以上）は[GitHubリリース](https://github.com/User278-bot/GhostModProject/releases)ページから別途ダウンロードできます。

```bash
java -jar GhostModServer-<version>.jar
```

デフォルトポート: `8887`

## 📖 詳細情報

セットアップ手順、サーバー設定オプション、開発者向けドキュメントについては、[GitHubリポジトリ](https://github.com/User278-bot/GhostModProject)をご覧ください。

## ⚠️ 注意

本プロジェクトはAIツール（Antigravity & Google AI Studio）を活用して開発されています。コードはMITライセンスの下、「AS IS（現状のまま）」で提供されます。
