# KNOWN_ISSUES — シンプルライト

未解決・実機依存・確認待ちの事項をここに記録する。仕様は `CURRENT_SPEC.md` を参照する。

## 現在の未確認事項

### 実機UI確認

Blackview MEGA 1を含む実機で、MUSHIN Glass導入後の以下を最終目視確認する必要がある。

- 文字切れ
- 小型画面での余白
- MUSHINフォントの見え方
- ガラス濃度72の視認性
- キーカラー / 文字色 / ガラス濃度変更後の全画面反映
- RecyclerViewや検索結果など後から生成されるUIの反映
- Compose画面のフラット感、角丸、影
- カメラ映像を変えず操作帯だけMUSHIN Glassになっていること
- 外観設定画面
- ランチャーアイコン
- ダイアログとシステムバーInsets

APKビルドと静的監査はGitHub Actionsで確認するが、実機目視は別項目として未確認扱いとする。

### 端末依存のLED強度

LED強度制御はAndroid 13以降かつ対応ハードウェアでのみ実際の強度差が出る。2段階端末や強度制御非対応端末では、UI上の弱 / 中 / 強と物理出力が1対1にならない場合がある。

### メーカー独自省電力

メーカー独自の省電力・バックグラウンド制御により、自動消灯タイミングやプロセス継続性に差が出る可能性がある。アプリ内タイマーと `setAndAllowWhileIdle()` で対応しているが、全メーカーでの同一挙動は保証しない。

### カメラ競合

他アプリがカメラ資源を利用した場合、OS判断でトーチが消灯・利用不可になることがある。本アプリはTorchCallbackで状態へ追従する。

## 解消済み

- 共通ランタイムがActivity初期表示時のViewしか十分に扱えず、RecyclerView行、通信後の検索結果、ページ移動後の再生成UI、プログラム生成ダイアログなどが旧色・旧フォントへ戻る場合があった問題。限定3回の再適用と `MushinGlass.applyToView()` を追加し、必要な生成箇所から明示適用するよう修正（2026-10-25）
- Composeアプリの一部が共通 `MushinGlassTheme` の複製や標準Material3の強い塗りボタンを残していた問題。共通Themeを正本へ統一し、主要操作をフラットなOutlined/Surface表現へ寄せた（2026-10-25）
- `CheckBox` / `Switch` がButton系として扱われてガラス長方形へ変形し得た問題。CompoundButtonを先に処理して形状を保持するよう修正（2026-10-25）
- camelCaseの `statusPanel` / `controlPanel` 等を共通ガラス判定が拾えなかった問題。View IDを小文字正規化して判定するよう修正（2026-10-25）
- カメラのシャッター、録画赤、期限警告など意味色・専用形状を共通テーマが上書きし得た問題。`keep_text_color` / `keep_mushin_shape` 例外を追加（2026-10-25）
- MUSHIN GlassのManifest解析が自己終了形式の `<activity ... />` を誤ってMAIN/LAUNCHER Activityとして扱い、ランチャーaliasが設定画面など誤ったActivityを向く場合があった問題。自己終了Activityを解析候補から除外し、元のMAIN/LAUNCHER ActivityをtargetActivityとして使用するよう修正（2026-10-25）
- Quick Settings追加キャンセル時の無反応
- 権限取消後にタイル状態が古いまま残る問題
- 設定ダイアログ下部の3ボタンによる小型画面の窮屈さ
- Android 14以降のQuick Settings起動方式
- Android 15 / 16 edge-to-edgeでの操作部被り
