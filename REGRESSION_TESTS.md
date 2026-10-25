# REGRESSION_TESTS — シンプルライト

`CURRENT_SPEC.md` の変更に合わせてこのチェックリストも更新する。

## ビルド

- [ ] Debug APKがビルドできる
- [ ] Android Lintが成功する
- [ ] Manifest / resource参照エラーがない

## 起動・権限

- [ ] 初回起動でCAMERA権限だけを要求する
- [ ] 権限許可後にライト操作できる
- [ ] 権限拒否後に適切な案内が出る
- [ ] 後から権限を外した場合に古いON状態を残さない

## ライト

- [ ] 弱 / 中 / 強 / OFFが操作できる
- [ ] 前回明るさを保存・復元する
- [ ] 起動時自動点灯ON/OFFが仕様どおり動く
- [ ] 画面ライトを開始・終了できる

## 自動消灯

- [ ] なし / 1分 / 5分 / 10分 / 30分を選べる
- [ ] 新しい点灯セッションに古い予約が干渉しない
- [ ] 手動OFF後に古い予約が不要な操作をしない
- [ ] 外部OFF / カメラ占有後に状態が追従する

## Quick Settings

- [ ] タイルからON/OFFできる
- [ ] 権限が無い場合に「権限が必要」と表示する
- [ ] Android 13以降のタイル追加導線が動く
- [ ] 追加キャンセル時に無反応にならない

## MUSHIN Glass

- [ ] MUSHINフォントが表示される
- [ ] 初期キーカラーが `#7FA98C`
- [ ] 初期文字色が黒
- [ ] 初期アイコンがモスグリーン `#436E6A`
- [ ] application icon / roundIconが有効なMUSHINアイコンresourceを参照する
- [ ] `IconTealGray` launcher aliasが初期状態で `enabled=true` になる
- [ ] launcher aliasの `targetActivity` が元のMAIN/LAUNCHER Activityと一致する
- [ ] Manifest内に自己終了形式のActivityがMAIN/LAUNCHERより前にあってもtargetActivityを誤認しない
- [ ] ガラス濃度初期値72
- [ ] キーカラー変更が初期画面だけでなく、画面再生成後・戻る操作後にも反映される
- [ ] 文字色変更が初期画面だけでなく、画面再生成後・戻る操作後にも反映される
- [ ] ガラス濃度変更が初期画面だけでなく、画面再生成後・戻る操作後にも反映される
- [ ] アプリアイコン設定が適用される
- [ ] RecyclerView / Listの後生成行にもMUSHINフォント・設定色・ガラス面が反映される
- [ ] 通信結果、検索結果、ページ移動後などユーザー操作後に追加されるViewへ設定が反映される
- [ ] プログラム生成ダイアログの入力欄・選択UIへ設定が反映される
- [ ] Compose画面のルートが `MushinGlassTheme` を通し、キーカラー・文字色・フォント・ガラス濃度に追従する
- [ ] Compose画面で旧式の強い塗りボタンや過剰なFAB影が残っていない
- [ ] CheckBox / Switchがガラス長方形へ変形せず、標準の操作形状を維持する
- [ ] SeekBar / ProgressBar / Spinnerが設定したキーカラーへ追従する
- [ ] `keep_text_color` を付けた録画赤・期限警告・状態色などが保持される
- [ ] `keep_mushin_shape` を付けたシャッター等の専用形状が保持される
- [ ] WebView内のWebサイト色を共通テーマが上書きしない
- [ ] ImageView / カメラプレビュー / SurfaceView / TextureView / GLSurfaceViewを共通テーマが再着色しない
- [ ] カメラ系アプリでは映像を保ったまま上部/下部操作帯・設定・一般操作だけがMUSHIN Glassになる
- [ ] 強い影や旧式の立体ボタンになっていない
- [ ] 主要カード/検索欄/設定セクションの角丸が過剰でない

## 小型画面・アクセシビリティ

- [ ] 320dp前後でも主要操作が画面外へ押し出されない
- [ ] 設定画面をスクロールできる
- [ ] 文字拡大時に主要操作が重ならない
- [ ] Android 15 / 16 edge-to-edgeでシステムバーと重ならない
- [ ] 文字切れがない
- [ ] 主要操作のタップ領域が不足しない

## 実行記録

2026-10-24 MUSHIN Glass rollout:

- Debug APK: PASS (GitHub Actions)
- Android Lint: PASS (GitHub Actions)
- APK artifact: PASS
- Blackview MEGA 1実機目視: NOT RUN — 実機画面確認待ち

2026-10-25 launcher icon repair:

- 17 Android apps rebuild: PASS (GitHub Actions)
- APK launcher icon static audit: PASS 17/17
- application icon / roundIcon = enabled `IconTealGray` resource: PASS 17/17
- `IconTealGray enabled=true`, `IconDefault enabled=false`: PASS 17/17
- launcher alias `targetActivity = MainActivity`: PASS 17/17
- launcher alias count: 21 variants per APK, PASS 17/17
- Blackview MEGA 1 launcher display: NOT RUN — 実機画面確認待ち

2026-10-25 full UI application repair:

- common runtime dynamic-view support: IMPLEMENTED
- Compose shared-theme audit: IMPLEMENTED / individual apps migrated where required
- recycled/dynamic row explicit styling: IMPLEMENTED where required
- camera preview exclusion + glass chrome: IMPLEMENTED where required
- final 17-app rebuild: PENDING — version `2026-10-25.2` rollout後に実施
- Blackview MEGA 1 visual QA: NOT RUN — 実機画面確認待ち
