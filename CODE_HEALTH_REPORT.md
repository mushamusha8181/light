# シンプルライト コード健康診断（最終確認）

## 結論
全ソース、Manifest、Gradle設定、XMLリソース、権限、Quick Settings、自動消灯、画面ライト、小型スマホUIを再確認しました。静的検査で確認できる範囲では、参照切れ・XML構文エラー・Manifestのコンポーネント参照切れ・明らかな重複処理はありません。

## 今回追加で修正した項目

### 1. Quick Settings追加をキャンセルしたときの無反応
`TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED` が返った場合に何も表示していなかったため、追加されなかった場合は失敗案内を表示するよう修正しました。

### 2. 権限を後から外した場合のタイル表示
Quick Settingsを開いた時点でCAMERA権限がなければ、古いON状態を破棄し、自動消灯予約を停止して「権限が必要」と表示するようにしました。タップすればアプリの権限導線へ進めます。

### 3. 小型画面の設定ダイアログ
「クイック設定に追加 / キャンセル / 保存」の3ボタンをダイアログ下部に並べると、小さい画面や大きな文字で窮屈になります。クイック設定追加ボタンを本文内の48dpボタンへ移動し、下部は「キャンセル / 保存」の2つだけにしました。

### 4. 読みやすさとアクセシビリティ
補足表示を11spから12spへ調整し、装飾用の丸印が読み上げ対象にならないよう設定しました。

### 5. Gradle自己ブートストラップ
`gradlew` / `gradlew.bat` は標準Wrapper JAR方式ではなく自己ブートストラップ方式です。パス解決、途中ダウンロードファイル、展開ツール不足時のエラー処理を強化し、Gradle 8.11.1のZIPを公式SHA-256で検証するようにしました。

## 既存機能の再確認

| 機能 | 判定 | 備考 |
|---|---|---|
| 弱・中・強 | OK | API 33以降＋対応端末で実強度変更 |
| OFF | OK | CameraManager経由 |
| 前回の明るさ記憶 | OK | SharedPreferences |
| 起動時自動点灯 | OK | Activity再生成時の二重実行を防止 |
| 自動消灯 | OK | Handler + idle対応アラーム、セッション/所有確認あり |
| Quick Settings | OK | API 34以降のPendingIntent起動へ対応 |
| 画面ライト | OK | 最大画面輝度 + 画面ON維持 |
| 初回権限 | OK | CAMERAのみ初回要求 |
| 拒否後の再許可 | OK | rationale / アプリ設定導線あり |
| 小型スマホUI | OK | 主要ボタン48dp、ScrollView、Insets対応 |

## TorchCallbackが3か所ある理由
以下は単純な重複ではなく、ライフサイクルが異なるため意図的に分離しています。
- MainActivity：画面が見えている間のUI同期
- QuickTileService：クイック設定パネルが状態を必要とする間の同期
- AutoOffScheduler：自動消灯予約中に外部OFF/カメラ占有を検知するための監視

MainActivityとTileServiceは不要になった時点で登録解除し、AutoOffSchedulerも予約終了・キャンセル時に解除します。常時3本を監視し続ける構成ではありません。

## 権限
Manifestのユーザー向け実行時権限はCAMERAだけです。以下は使用していません。
- INTERNET
- POST_NOTIFICATIONS
- 位置情報
- マイク
- ストレージ
- SCHEDULE_EXACT_ALARM
- フォアグラウンドサービス

## ビルド設定
- Android Gradle Plugin 8.10.1
- Gradle 8.11.1
- Java 17
- compileSdk / targetSdk 36
- minSdk 23
- versionCode 4 / versionName 3.1

AGP 8.10系はAPI 36をサポートし、必要Gradleは8.11.1です。

## 静的チェック結果
- XMLパース：OK（12ファイル）
- string / color / drawable / layout参照切れ：0
- Manifestコンポーネント参照切れ：0
- Java括弧対応：OK
- `gradlew` シェル構文：OK
- TODO / FIXME / HACK残存：0
- 不要な危険権限：なし

## 実機でしか確定できない項目
この実行環境にはAndroid SDKと実機がないため、以下は最終的にAndroid Studio / 実機で確認が必要です。
1. `assembleDebug` と Android Lint
2. Android 6 / 13 / 14 / 15 / 16での起動
3. LED強度非対応 / 2段階 / 3段階以上の各端末
4. カメラアプリ使用中のライト競合
5. メーカー独自の省電力制御下での自動消灯
6. 320dp前後の画面と大きなフォント

## Android仕様上の制約
トーチはアプリの排他的所有物ではありません。他アプリもトーチ状態を変更でき、カメラ資源が使えなくなればOSが消灯・使用不可へ変更します。本アプリはTorchCallbackでその変化へ追従しますが、端末メーカー実装まで完全に同一にはできません。
