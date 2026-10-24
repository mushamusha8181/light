# MUSHIN Glass Design System

`docs/DESIGN_SYSTEM.md` をムシャムシャ系アプリの共通デザイン仕様の正本とする。

## 1. 基本思想

MUSHIN Glass は **リキッドグラス × フラットデザイン** を基本とする。

- 情報構造と操作はフラットで明快にする。
- ガラス表現はカード、検索欄、ダイアログ、下部ナビ、設定セクション、アイコン背景など前面要素へ控えめに使う。
- 全面をガラス化しない。
- 読みにくくなるほど、ぼかし・透明度・影を増やさない。
- アプリ固有の機能構成は維持し、見た目・余白・色・タイポグラフィ・操作面の言語を統一する。
- 小さいスマートフォンでも窮屈にならないことを優先する。

## 2. 基準バージョン

- Baseline: `2026-10-24.2`
- 初期キーカラー: `green_light` / `#7FA98C`
- 初期文字色: `black` / `#000000`
- 初期アプリアイコン: `teal_gray` / `#436E6A`
- 初期ガラス濃度: `72`

## 3. タイポグラフィ

標準フォントは **MUSHIN / 無心**。

- 太字だけに頼って情報の強弱を付けない。
- 文字サイズ、余白、色、配置で階層を表現する。
- Androidでは生成された `font/mushin` をMaterial Typography全体へ適用する。
- WebView本文、Webコンテンツ、画像内文字など外部コンテンツの書体は上書きしない。

## 4. 色

外観設定は `SharedPreferences("color_settings")` を使う。

- `palette`: キーカラー。初期値 `green_light`
- `text_color`: 文字色。初期値 `black`
- `glass_opacity`: ガラス濃度。初期値 `72`、範囲 `35..95`

派生色:

- 背景: `mix(primary, #FFFFFF, 0.82)`
- 上下の帯: `mix(primary, background, 0.70)`
- 一覧背景: `mix(primary, #FFFFFF, 0.90)`
- 行の下地: `mix(primary, #FFFFFF, 0.92)`
- 中間色: `mix(primary, background, 0.45)`
- 補助色: `mix(foreground, background, 0.45)`

`mix(a,b,t)` はRGB各成分を `a + (b-a) * t` で混ぜる。

## 5. 形状とガラス面

Compose標準Shape:

- extraSmall: `8dp`
- small: `10dp`
- medium: `14dp`
- large: `18dp`
- extraLarge: `24dp`

基本ルール:

- 主要カードは過度なピル形状にしない。
- 枠線は細く弱くする。
- 影は非常に弱く、立体的な旧式ボタン表現を避ける。
- 操作面はカード、行、アイコン、ガラス面として表現する。
- 大きすぎるボタンや余白で小型端末を圧迫しない。

## 6. アイコン

アプリアイコンは `SharedPreferences("app_icon")` を使う。

- `mode`: `default` / `follow` / パレットID
- 初期値: `teal_gray`
- `applied`: 実際に有効化したID
- `follow`: 現在のキーカラーへ追従

標準アイコンは白背景 + モスグリーン `#436E6A` のMマーク。

写真、ロゴ、作品画像など色を変えると意味が壊れるアイコンには、無理に色変更機能を適用しない。

## 7. 文字色の例外

以下は必要に応じ既存色を維持する。

- `file_name` と一覧操作行
- `NOW PLAYING` / `再生中` 見出し
- `keep_text_color` タグを持つ部品
- Webページ本文
- カメラ映像、画像、動画など外部視覚コンテンツ

## 8. 設定画面

外観設定では少なくとも次を扱う。

- キーカラー
- 文字色
- ガラス濃度
- アプリアイコン色（適用可能なアプリ）

設定変更後はActivity再生成またはCompose再構成で画面へ反映する。

## 9. 小型スマートフォン

- 主要操作はタップしやすさを保ちつつ、縦方向を占有しすぎない。
- 設定画面はスクロール可能にする。
- 文字拡大時にも主要操作が画面外へ押し出されないようにする。
- システムバーInsetsを考慮する。
- ダイアログ下部へ多数のボタンを横並びにしない。

## 10. 実装

Android共通Gradle層:

- `mushin-glass.gradle`
- `design-system/mushin-glass.gradle`

両パスは同じ生成仕様を維持する。

共通ランタイム:

- `app/src/main/java/jp/mushamusha/mushinglass/MushinGlass.java`

Compose正本:

- `design-system/MushinGlassCompose.kt`

Composeアプリは `MushinGlassTheme { ... }` を標準入口とし、`MushinGlassCompose.Theme { ... }` も互換入口として維持する。

## 11. 回帰確認

MUSHIN Glassを変更した場合は、影響するAndroidアプリで少なくとも以下を確認する。

- Debug APKビルド
- Android Lint（ワークフローが対応している場合）
- 起動
- メイン画面
- 設定画面
- ダイアログ / ナビゲーション
- 文字切れ
- 小型画面での余白
- MUSHINフォント
- キーカラー / 文字色 / ガラス濃度
- ランチャーアイコン
- 既存の主要機能がUI変更前と同じく動くこと

ビルド未実行・失敗・実機未確認の項目は、完了扱いにせず明示する。
