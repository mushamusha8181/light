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
- [ ] キーカラー変更が反映される
- [ ] 文字色変更が反映される
- [ ] ガラス濃度変更が反映される
- [ ] アプリアイコン設定が適用される
- [ ] 強い影や旧式の立体ボタンになっていない

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
