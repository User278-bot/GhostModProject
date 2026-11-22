# Security Policy

[日本語版はこちら](#セキュリティポリシー日本語)

---

## Supported Versions

Security updates are provided **only for the latest releases** of GhostMod (`ghost-mod`) and GhostModServer (`ghost-server`).

- For **`ghost-mod`**, support is determined by the **Mod version** (not the Minecraft version): **only the latest Mod version** (`mod/v*`) is supported. All Minecraft versions bundled in that latest Mod release are covered, while older Mod versions are not supported.
- For **`ghost-server`**, **only the latest Server version** (`server/v*`) is supported.

Please keep both your Mod and Server updated to their latest versions.

| Component | Version | Supported |
| :--- | :--- | :---: |
| `ghost-mod` (Minecraft Mod) | **Latest Mod version** (`mod/v*`, across all MC versions included in that release) | ✅ |
| `ghost-mod` (Minecraft Mod) | Older Mod versions | ❌ |
| `ghost-server` (Sync Server) | **Latest Server version** (`server/v*`) | ✅ |
| `ghost-server` (Sync Server) | Older Server versions | ❌ |
| `ghost-fake_client` | Testing / debugging tool (not for production) | ❌ |

---

## Reporting a Vulnerability

**Please do NOT report security vulnerabilities through public GitHub Issues, Pull Requests, Discussions, or Modrinth/CurseForge comments.**

Instead, please report them privately using GitHub's **Private Vulnerability Reporting** feature:

1. Go to the [Security Advisories page](https://github.com/User278-bot/GhostModProject/security/advisories/new) of this repository.
2. Provide as much detail as possible, including:
   - Affected component (`ghost-mod` / `ghost-server` / `ghost-network`, etc.) and version
   - Minecraft / Java version (if applicable)
   - Steps to reproduce or a proof of concept (PoC)
   - Potential impact and any suggested remediation (if known)

### Response Expectations

This project is maintained by an individual developer on a **best-effort basis**:

- Initial acknowledgment is typically provided within a few days to one week.
- Once confirmed, a fix will be prepared privately and released across GitHub (and Modrinth / CurseForge for the Mod).
- After a patched version is available, details may be published via a GitHub Security Advisory (coordinated disclosure).

---

## Security Model & Out of Scope

Ghost Mod Project is designed primarily for **private, small-group play among trusted friends** (e.g., exploring single-player adventure maps together). Please keep the following design assumptions and operational responsibilities in mind:

- **Transport & Encryption**: Communication between the Mod and `ghost-server` uses WebSocket with CHAP authentication and application-layer AES-GCM encryption derived from the server password. However, `ghost-server` does not natively terminate TLS (`wss://`).
- **Password Management**: Always configure a strong, unique password (`--password` or `server.properties`) when exposing the server outside a local network. Do **not** expose a server using weak passwords or the default auto-generated password.
- **Network Exposure**: Server operators are solely responsible for port forwarding, firewall rules, and network security. When exposing a server over the internet, using a reverse proxy with TLS, a VPN, or a tunneling service is strongly recommended.

The following are generally considered **out of scope** as software vulnerabilities:

- Misconfigurations on the operator's side (weak/empty passwords, exposed admin consoles, improper firewall/router settings)
- Large-scale public-server DDoS resilience beyond the built-in basic per-IP rate limiting
- Issues originating from third-party modified builds or unofficial redistribution channels
- Security vulnerabilities in unsupported/outdated versions

This software is provided "AS IS" without warranty under the [MIT License](LICENSE).

---

# セキュリティポリシー（日本語）

## サポート対象バージョン

セキュリティ修正は、**GhostMod（`ghost-mod`）および GhostModServer（`ghost-server`）の最新リリースのみ**を対象に提供します。

- **`ghost-mod`** については、Minecraft のバージョンではなく **Mod 自体のバージョン**を基準とし、**Mod の最新バージョン（最新の `mod/v*` リリース）のみ**をサポート対象とします。その最新 Mod リリースに含まれる各対応 Minecraft バージョン用ビルドはいずれも対象ですが、Mod の旧バージョンはサポート対象外です。
- **`ghost-server`** については、**サーバーの最新バージョン（最新の `server/v*` リリース）のみ**をサポート対象とします。

Mod・サーバーともに常に最新バージョンへ更新してご利用ください。

| コンポーネント | バージョン | サポート状況 |
| :--- | :--- | :---: |
| `ghost-mod`（Minecraft Mod） | **Mod の最新バージョン**（最新 `mod/v*` リリースに含まれる全対応 MC バージョン） | ✅ |
| `ghost-mod`（Minecraft Mod） | Mod の旧バージョン | ❌ |
| `ghost-server`（同期サーバー） | **サーバーの最新バージョン**（最新 `server/v*` リリース） | ✅ |
| `ghost-server`（同期サーバー） | サーバーの旧バージョン | ❌ |
| `ghost-fake_client` | テスト・デバッグ用ツール（本番公開非想定） | ❌ |

---

## 脆弱性の報告方法

**セキュリティ上の脆弱性については、公開の GitHub Issue・Pull Request・Discussions、および Modrinth / CurseForge のコメント欄には絶対に記載しないでください。**

脆弱性を発見した場合は、GitHub の **Private Vulnerability Reporting（非公開の脆弱性報告）** 機能よりご報告ください。

1. 本リポジトリの [Security Advisories 報告ページ](https://github.com/User278-bot/GhostModProject/security/advisories/new) を開きます。
2. 可能な範囲で以下の情報を記載してください。
   - 対象コンポーネント（`ghost-mod` / `ghost-server` / `ghost-network` 等）およびバージョン
   - Minecraft / Java のバージョン（該当する場合）
   - 再現手順または検証コード（PoC）
   - 想定される影響範囲、修正案（あれば）

### 対応の目安と開示方針

本プロジェクトは個人開発のため、**ベストエフォートでの対応**となります。

- 初回の確認・返信には、通常数日〜1週間程度かかる場合があります。
- 脆弱性が確認された場合、非公開で修正を進め、GitHub（Mod については Modrinth / CurseForge を含む）にて修正版をリリースします。
- 修正版の公開後、必要に応じて GitHub Security Advisory として詳細を開示します（協調的開示）。

---

## セキュリティモデルとスコープ外事項

Ghost Mod Project は、主に**友人同士など信頼できる少人数でのプライベート利用**（一人用配布マップの擬似マルチプレイ等）を前提に設計されています。運用にあたっては、以下の前提および利用者側の責任範囲をご理解ください。

- **通信保護と暗号化**: Mod と `ghost-server` 間の通信は、WebSocket 上で CHAP 認証およびサーバーパスワード由来の AES-GCM によるアプリケーション層暗号化を行っています。ただし、`ghost-server` 単体では TLS（`wss://`）終端機能は提供していません。
- **パスワード管理**: サーバーをローカルネットワーク外へ公開する場合は、十分に長く推測されにくいパスワード（`--password` または `server.properties`）を必ず設定してください。弱いパスワードや、未指定時の自動生成パスワードのまま外部公開しないでください。
- **ネットワーク公開**: ポート開放・ファイアウォール設定・ネットワーク構成の安全性はサーバー運用者の責任となります。インターネット越しに利用する場合は、TLS 対応リバースプロキシ、VPN、またはトンネリングサービスの併用を推奨します。

以下は原則として本ソフトウェアの脆弱性対応の**スコープ外**とします。

- 運用者側の設定不備に起因する問題（弱いパスワード、ファイアウォールやルーターの設定ミス等）
- 内蔵の簡易 IP レート制限を超えるような、不特定多数向け大規模公開サーバー水準の高度な DDoS 対策
- 非公式の改造ビルドや第三者配布物における問題
- サポート終了済み（旧バージョン）における脆弱性

本ソフトウェアは [MIT License](LICENSE) に基づき、無保証（AS IS）で提供されます。
