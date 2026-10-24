# CURRENT_SPEC — シンプルライト

このファイルをシンプルライトの現在仕様の正本とする。READMEは利用者向け説明であり、本ファイルを置き換えない。

## アプリ目的

小型スマートフォンでも迷わず使える、必要機能に絞ったAndroidライトアプリ。

## 対応環境

- minSdk: 23
- compileSdk / targetSdk: 36
- Java: 17
- Android Gradle Plugin: 8.10.1
- Gradle: 8.11.1

## ライト機能

- LEDライト: 弱 / 中 / 強 / OFF
- Android 13以降かつ対応端末ではTorchStrengthLevelを利用する
- 強度制御非対応端末では弱 / 中 / 強は同じ物理強度になり得る
- 前回選択した明るさを保存する
- 設定により起動時に前回明るさで自動点灯できる
- 画面全体を白くする画面ライトを提供する

## 自動消灯

- なし / 1分 / 5分 / 10分 / 30分
- 通常時はアプリ内タイマーを使う
- スリープ時の補助として `AlarmManager.setAndAllowWhileIdle()` を使う
- 古い予約が新しい点灯セッションへ干渉しないよう、セッションと所有状態を検証してから消灯する

## Quick Settings

- Quick SettingsタイルからON/OFFできる
- Android 13以降ではアプリ設定画面から追加リクエストを出せる
- Android 12L以前ではOSの編集画面から手動追加する
- Android 14以降はPendingIntent版 `startActivityAndCollapse()` を使う
- CAMERA権限が無い場合は「権限が必要」と表示し、アプリの権限導線を開く

## 権限

ユーザー向け実行時権限は `android.permission.CAMERA` のみ。

使用しないもの:

- 位置情報
- マイク
- ストレージ
- 通知
- INTERNET
- SCHEDULE_EXACT_ALARM
- フォアグラウンドサービス

## MUSHIN Glass

共通デザイン仕様は `docs/DESIGN_SYSTEM.md` を正本とする。

現在基準:

- MUSHIN / 無心フォント
- 初期キーカラー: `green_light #7FA98C`
- 初期文字色: `black #000000`
- 初期アプリアイコン: `teal_gray #436E6A`
- 初期ガラス濃度: `72`
- フラットな情報構造 + 控えめな半透明ガラス面
- 小型画面優先

## UI

- 主要操作の高さは48dpを基本とする
- 設定画面はスクロール可能
- Android 15 / 16のedge-to-edgeとInsetsを考慮する
- 小型端末や大きな文字で操作が押し出されないようにする
- ダイアログ下部へ多数の操作ボタンを横並びにしない

## 状態監視

TorchCallbackは用途別に分離する。

- MainActivity: 画面表示中のUI同期
- QuickTileService: Quick Settings表示中の同期
- AutoOffScheduler: 自動消灯予約中の外部OFF / カメラ占有検知

各監視は不要になった時点で解除し、常時重複監視し続けない。

## 完了条件

変更完了時は、対象変更に応じて以下を満たす。

- Debug APKビルド成功
- Android Lint成功（適用可能な場合）
- 回帰チェック更新・実施
- READMEの現状との整合確認
- `KNOWN_ISSUES.md` の確認・更新
- 実行できなかった確認は未確認として明示する
