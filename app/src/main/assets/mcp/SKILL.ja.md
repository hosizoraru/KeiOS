# KeiOS MCP Skill

## サービス識別
- App: {{APP_LABEL}} ({{APP_PACKAGE}})
- バージョン: {{APP_VERSION}}
- MCP サーバー: {{SERVER_NAME}}
- ローカル endpoint: {{LOCAL_ENDPOINT}}
- LAN endpoint: {{LAN_ENDPOINTS}}

## 接続入口
1. `keios.health.ping` を呼び出して接続を確認します。
2. `keios.mcp.runtime.status` で実行状態、endpoint、接続クライアント、Token 状態を読み取ります。
3. `keios.mcp.runtime.config(mode=auto)` でクライアントにインポートできる JSON を生成します。
4. `{{RESOURCE_OVERVIEW_URI}}` を読み取り、ツールのグループ概要を確認します。
5. タスクに合わせて `{{RESOURCE_SKILL_URI}}` または `keios://skill/tool/{tool}` を読み取ります。

## 設定インポート
- 既定リソース: `{{RESOURCE_CONFIG_URI}}`
- mode テンプレート: `{{RESOURCE_CONFIG_TEMPLATE_URI}}`
- Bootstrap prompt: `{{PROMPT_BOOTSTRAP}}`
- Claw 接続ツール: `keios.mcp.claw.skill.guide(mode=auto)`
- 同一端末クライアントは `local`、別端末デバッグは `lan`、利用可能な endpoint をまとめて生成する場合は `auto` を使います。

## ツール機能
{{TOOL_LIST}}

## Home 概要
- `keios.home.overview.snapshot`: MCP、GitHub、ブルーアーカイブの Home 概要カードスナップショット。

## 実行状態と環境
- `keios.health.ping`: 接続確認。常に `pong` を返します。
- `keios.app.info`: アプリラベル、パッケージ名、バージョン、Shizuku API レベル。
- `keios.app.version`: versionName と versionCode。
- `keios.shizuku.status`: 現在の Shizuku 状態。
- `keios.mcp.runtime.status`: MCP 実行状態、ポート、パス、Token 状態、クライアント数。
- `keios.mcp.runtime.logs`: MCP ログ。`limit` は 1 から 200。
- `keios.mcp.runtime.config`: streamable HTTP クライアント設定を生成します。
- `keios.mcp.claw.skill.guide`: Claw インポート JSON、リソース URI、完全な Skill テキストを出力します。

## OS とシステム確認
- `keios.os.cards.snapshot`: OS ページのカード表示、展開状態、キャッシュサイズ、推定値。
- `keios.os.activity.cards`: Activity カード一覧。`query`、`onlyVisible`、`limit` に対応します。
- `keios.os.shell.cards`: Shell カード一覧。`query`、`onlyVisible`、`includeOutput`、`limit` に対応します。
- `keios.os.cards.export`: Activity または Shell カード JSON を `target=activity|shell|all` でエクスポートします。
- `keios.os.cards.import`: Activity または Shell カード JSON をプレビューまたはマージします。既定は `apply=false` のプレビューです。
- `keios.system.topinfo.query`: キャッシュ済みシステム TopInfo を `query` と `limit` で検索します。

## GitHub 追跡
- `keios.github.tracked.snapshot`: 追跡数、キャッシュヒット、方式、Token 状態、更新間隔。
- `keios.github.tracked.list`: 追跡中リポジトリ一覧。`repoFilter` と `limit` に対応します。
- `keios.github.tracked.export`: 追跡中リポジトリを JSON でエクスポートします。`repoFilter` に対応します。
- `keios.github.tracked.import`: 追跡中リポジトリをプレビューまたはマージします。既定は `apply=false` のプレビューです。
- `keios.github.tracked.summary`: `mode=cache|network` でキャッシュまたはオンライン確認結果を要約します。
- `keios.github.tracked.check`: `repoFilter`、`onlyUpdates`、`limit` で更新をオンライン確認します。
- `keios.github.tracked.cache.clear`: 確認キャッシュとリリースアセットキャッシュを削除します。

## GitHub 共有インポート
- `keios.github.share.parse`: 共有テキストから GitHub repo、release、tag、APK リンクを解析します。
- `keios.github.share.resolve`: 現在の方式で共有リンクを解決し、APK アセット候補を一覧します。
- `keios.github.share.pending`: `clear=true` でインストール前の共有追跡状態を読み取り、または削除します。

## ブルーアーカイブキャッシュ
- `keios.ba.snapshot`: AP、カフェ、通知しきい値、更新間隔、先生情報。
- `keios.ba.calendar.cache`: イベントカレンダーキャッシュ。`serverIndex`、`includeEntries`、`limit` に対応します。
- `keios.ba.pool.cache`: 募集キャッシュ。`serverIndex`、`includeEntries`、`limit` に対応します。
- `keios.ba.guide.catalog.cache`: 生徒名簿カタログキャッシュ。`tab=all|student|npc` に対応します。
- `keios.ba.guide.cache.overview`: 生徒情報詳細キャッシュサイズと最終同期時刻。
- `keios.ba.guide.cache.inspect`: URL で生徒詳細キャッシュの完全性とセクション統計を検査します。
- `keios.ba.guide.media.list`: 生徒情報キャッシュからギャラリーとボイスメディアを一覧します。`kind=all|gallery|voice|image|video|audio` に対応します。
- `keios.ba.guide.bgm.favorites`: `action=list|export|import` でメモリアルロビー BGM お気に入りを読み取り、エクスポート、またはインポートします。
- `keios.ba.cache.clear`: `scope` と `url` でブルーアーカイブと GitHub のキャッシュを削除します。

## 推奨フロー
1. 実行診断: `keios.health.ping` -> `keios.mcp.runtime.status` -> `keios.mcp.runtime.logs` -> `keios.shizuku.status`
2. 設定インポート: `keios.mcp.runtime.config(mode=auto)` -> `{{RESOURCE_CONFIG_URI}}` を読み取り -> `keios.mcp.claw.skill.guide`
3. Home 確認: `keios.home.overview.snapshot` -> MCP、GitHub、ブルーアーカイブ状態へ掘り下げ
4. OS 確認: `keios.os.cards.snapshot` -> `keios.os.activity.cards` -> `keios.os.shell.cards` -> `keios.os.cards.export`
5. GitHub 確認: `keios.github.tracked.snapshot` -> `keios.github.tracked.summary(mode=cache)` -> `keios.github.tracked.check(onlyUpdates=true)`
6. GitHub 共有インポート: `keios.github.share.parse` -> `keios.github.share.resolve` -> `keios.github.share.pending`
7. ブルーアーカイブ確認: `keios.ba.snapshot` -> `keios.ba.guide.cache.inspect` -> `keios.ba.guide.media.list` -> `keios.ba.guide.bgm.favorites`
8. インポート書き込み: まず `apply=false` でプレビューし、件数を確認してから `apply=true` を使います。
9. キャッシュ削除: 具体的な `scope` を指定し、対応する snapshot または cache ツールで確認します。

## 引数と出力
- `limit` は 20 から 80 で始め、大きな監査時だけ増やします。
- `repoFilter` は owner/repo、パッケージ名、アプリラベルに対応します。
- `serverIndex` は 0 から 2。空欄時は現在のブルーアーカイブ設定を使います。
- `includeOutput=true` は Shell 出力サマリーを追加します。
- `includeEntries=true` はキャッシュ項目を展開し、サンプル監査に使えます。
- インポート系ツールは既定でプレビューします。書き込みには明示的な `apply=true` が必要です。
- 出力は `key=value` 行と安定したリスト行を優先し、呼び出し側がレポートやログへ保存しやすい形にします。
