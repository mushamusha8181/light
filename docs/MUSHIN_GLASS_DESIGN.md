# MUSHIN Glass Design System

このリポジトリは、ムシャムシャのAndroidアプリ共通UI「MUSHIN Glass」を使用します。

## 共通ルール
- フォント: MUSHIN（無心）
- 基本: 白ベース + 薄い緑の光 + 半透明ガラス面 + 黒文字
- 初期キーカラー: `green_light` (`#7FA98C`)
- 初期文字色: `black` (`#000000`)
- 初期アプリアイコン: `teal_gray` (`#436E6A`)
- 角丸、細い白境界、大きめの余白を共通化
- 従来型の強い塗りつぶしボタンを避け、ガラス面・行・アイコンを操作面にする
- WebView本文、カメラ映像、画像そのものの色は変更しない

## 保存
外観は `SharedPreferences("color_settings")` に保存します。
- `palette`: キーカラー。初期値 `green_light`
- `text_color`: 文字色。初期値 `black`
- `glass_opacity`: ガラスの濃さ。初期値 72（35〜95）

アプリアイコンは `SharedPreferences("app_icon")` に保存します。
- `mode`: `default` / `follow` / 配色ID
- 初期値: `teal_gray`
- `applied`: 実際に有効化した配色ID

## 色生成
- 背景: `mix(primary, #FFFFFF, 0.82)`
- 上下の帯: `mix(primary, background, 0.70)`
- 一覧背景: `mix(primary, #FFFFFF, 0.90)`
- 行の下地: `mix(primary, #FFFFFF, 0.92)`
- 中間色: `mix(primary, background, 0.45)`
- 補助色: `mix(foreground, background, 0.45)`

`mix(a,b,t)` は RGB 各成分を `a + (b-a) * t` で混ぜます。

## 文字色の例外
- `file_name` と一覧の操作行は黒固定
- `NOW PLAYING` / `再生中` 見出しは黒固定
- `keep_text_color` タグの部品は既存色を維持
- Webページ本文にはアプリ文字色を適用しない

## MUSHINフォント
ビルド時に作者公式配布ZIPから取得し、生成リソース/assetとしてAPKに埋め込みます。
