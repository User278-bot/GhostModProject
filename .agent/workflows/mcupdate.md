---
description: Modが対応するMinecraftバージョンを追加する手順
---

# Minecraftバージョン追加ワークフロー

このワークフローでは、StoneCutterプラグインを使用したマルチバージョンModプロジェクトに新しいMinecraftバージョンのサポートを追加する手順を説明します。

> [!IMPORTANT]
> 作業前に、追加するバージョンに対応するFabric API、Fabric Loader、Cloth Config、ModMenuの各ライブラリバージョンを調査してください。
> 各ライブラリの対応バージョンは以下で確認できます:
> - Fabric API: https://modrinth.com/mod/fabric-api/versions
> - Cloth Config: https://modrinth.com/mod/cloth-config/versions
> - ModMenu: https://modrinth.com/mod/modmenu/versions

---

## 前提知識

### プロジェクト構成
```
ghost-mod/
├── stonecutter.gradle.kts    # active版・パラメータ定義
├── build.gradle.kts          # 共通ビルドスクリプト
├── gradle.properties         # 共通Modプロパティ
├── src/                      # 共有ソースコード（StoneCutterコメントで分岐）
│   ├── main/resources/       # fabric.mod.json 等
│   └── client/java/          # クライアントコード
├── versions/
│   ├── 1.19.2/gradle.properties  # バージョン固有の依存関係
│   ├── 1.20.1/gradle.properties
│   ├── 1.20.6/gradle.properties
│   ├── 1.21.4/gradle.properties
│   └── 1.21.11/gradle.properties
settings.gradle.kts           # StoneCutter versions() 定義
```

### StoneCutterコメント構文
ソースコード内でバージョン分岐を記述する構文:
```java
// 条件付きコードブロック（行コメント形式）
/*? >=1.20.6 {*/
// 1.20.6以降で有効なコード
/*?} else {*/
/*  1.20.5以前で有効なコード（コメントアウト）
*///?}

// 条件付きインポート（単一行）
/*? >=1.19.3 {*/
import net.minecraft.core.registries.BuiltInRegistries;
//?}
```
その他詳細は[公式サイトのwikiページ](https://stonecutter.kikugie.dev/wiki/)を参照すること。

### バージョン比較パターン（`build.gradle.kts`で使用）
```kotlin
stonecutter.eval(stonecutter.current.version, ">=1.20.6") // 比較演算子
```

---

## 手順

### ステップ1: 依存ライブラリのバージョン調査

追加するMinecraftバージョン（`X.Y.Z` とする）に対応する以下のライブラリバージョンを調べます:

| プロパティ名 | 説明 | 調査先 |
|---|---|---|
| `deps.fabric_api` | Fabric APIのバージョン | Modrinth/Maven |
| `cloth_config_version` | Cloth Configのバージョン | Modrinth/Maven |
| `mod_menu_version` | ModMenuのバージョン | Modrinth/Maven |

> [!TIP]
> 各ライブラリのModrinthページでMinecraftバージョンフィルターを使うと、対応バージョンを素早く確認できます。
> うまく行かない場合は[このサイト](https://linkie.shedaniel.dev/dependencies?loader=fabric&version=1.21.10)でもクエリを調節することで確認可能

// turbo-all

### ステップ2: `settings.gradle.kts` にバージョンを追加

`settings.gradle.kts` の `stonecutter` ブロック内 `versions()` に新しいバージョンを追加します:

```kotlin
stonecutter {
    create("ghost-mod") {
        versions("1.19.2","1.20.1","1.20.6","1.21.4","1.21.11","X.Y.Z")  // ← 新バージョンを追加
        vcsVersion = "1.19.2"  // ※ vcsVersionは変更しない
    }
}
```

> [!WARNING]
> `vcsVersion` は変更しないでください。これはGitで管理されるアクティブバージョンを指定するもので、通常は最も古い対応バージョンを指定します。

### ステップ3: バージョン固有の `gradle.properties` を作成

// turbo
`ghost-mod/versions/X.Y.Z/` ディレクトリを作成し、そこに `gradle.properties` ファイルを作成します。

既存バージョンのファイルを参考に、以下のフォーマットで作成してください:

```properties
# Dependencies
deps.fabric_api=<調査したFabric APIバージョン>
mod_menu_version=<調査したModMenuバージョン>
cloth_config_version=<調査したCloth Configバージョン>

# Minecraft dependency for fabric.mod.json
mod.mc_dep=<Minecraftバージョンの依存範囲>

# Release title for Modrinth and Curseforge
mod.mc_title=<リリースタイトルに表示するバージョン>
mod.mc_targets=<Modrinth/CurseForgeで対象とするバージョン（スペース区切り）>
```

#### `mod.mc_dep` の設定パターン:
- 単一バージョンのみ: `mod.mc_dep=1.21.4`
- 範囲指定（互換バージョンを含む場合）: `mod.mc_dep=>=1.20.5 <=1.20.6`

#### `mod.mc_targets` の設定パターン:
- 単一バージョン: `mod.mc_targets=1.21.4`
- 複数バージョン（スペース区切り）: `mod.mc_targets=1.20.5 1.20.6`

> [!NOTE]
> `mod.mc_dep` は `fabric.mod.json` の `depends.minecraft` に展開されます。
> `mod.mc_targets` はModrinth/CurseForgeへの公開時に対象バージョンとして使用されます。

### ステップ4: ソースコードのバージョン分岐を確認・更新

新しいバージョンで動作に影響が出る箇所がないか確認します。以下のファイルにStoneCutterコメントによるバージョン分岐があります:

| ファイル | 分岐の内容 |
|---|---|
| `EntityRegistration.java` | レジストリAPI（`>=1.19.3`でBuiltInRegistries使用） |
| `GhostPlayerEntity.java` | コンストラクタ引数、スキン取得API、PlayerSkin型（`>=1.20.1`, `>=1.20.6`） |
| `GhostEntitySynchronizer.java` | エンティティ追加API（`>=1.20.6`で`addEntity`使用） |
| `PlayerDataConverter.java` | Level取得API（`>=1.20.1`で`level()`メソッド使用） |
| `ToastNotifications.java` | SystemToast ID定数名（`>=1.20.6`で変更） |

新バージョンでAPIが変更されている場合は、適切なStoneCutterコメントを追加してください:

```java
// 新しい分岐を追加する例
/*? >=X.Y.Z {*/
newApiCall();
/*?} else {*/
/*oldApiCall();
*///?}
```

> [!CAUTION]
> Minecraftのメジャーアップデートでは、既存の条件分岐の閾値が正しく動作するか注意深く確認してください。
> 例: `>=1.20.6` の条件が新バージョンで既存コードへの影響がないかを確認します。

### ステップ5: `stonecutter.gradle.kts` の確認

`ghost-mod/stonecutter.gradle.kts` の `stonecutter active` が開発時にフォーカスしたいバージョンになっているか確認します。必要に応じて変更してください:

```kotlin
stonecutter active "X.Y.Z"  // 開発中のフォーカスバージョンを変更（任意）
```

> [!NOTE]
> `stonecutter active` は開発者のローカル環境でIDEが使用するバージョンを指定します。
> このファイルの `parameters` や `swaps` ブロックは通常変更不要です。

### ステップ6: `build.gradle.kts` のバージョン条件を確認

`ghost-mod/build.gradle.kts` にある `requiredJava` のバージョン分岐を確認します:

```kotlin
val requiredJava = when {
    stonecutter.eval(stonecutter.current.version, ">=1.20.6") -> JavaVersion.VERSION_21
    stonecutter.eval(stonecutter.current.version, ">=1.18") -> JavaVersion.VERSION_17
    stonecutter.eval(stonecutter.current.version, ">=1.17") -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}
```

新バージョンがJavaの要件を変更している場合（例: Java 25が必要になった場合）は、条件を追加してください。

### ステップ7: ビルド・検証

全バージョンのビルドを実行して、エラーがないことを確認します:

```shell
./gradlew build
```

> [!WARNING]
> `chiseledBuild` は使用できません。必ず `./gradlew build` で全体ビルドを行ってください。

特定バージョンのみテストする場合:
```shell
./gradlew ghost-mod:versions:X.Y.Z:build
```

### ステップ8: 動作確認（任意）

IDEでアクティブバージョンを切り替えて、Minecraftクライアントで動作を確認します:

1. `stonecutter.gradle.kts` の `stonecutter active` を新バージョンに変更
2. Gradleを再同期
3. Minecraftクライアントを起動し、Modの動作を確認

---

## チェックリスト

- [ ] 依存ライブラリのバージョンを調査した
- [ ] `settings.gradle.kts` の `versions()` にバージョンを追加した
- [ ] `versions/X.Y.Z/gradle.properties` を作成した
- [ ] ソースコードのStoneCutterコメント分岐を確認・更新した
- [ ] `build.gradle.kts` のJavaバージョン条件を確認した
- [ ] `./gradlew build` で全バージョンのビルドが成功した