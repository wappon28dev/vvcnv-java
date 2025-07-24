# VVCNV Java - Video Converter (Rust to Java Port)

このプロジェクトは、RustのFFmpeg動画変換ツール `vvcnv-rs` をJavaに移植したものです。

## 🚀 特徴

- **Rust → Java移植**: Rustの`vvcnv-rs`からJavaへの忠実な移植
- **Java 21活用**: モダンなJava機能（sealed class、record、pattern matching）を積極活用
- **Result型実装**: RustのResult型をJavaで再現（sealed interfaceを使用）
- **並列処理**: CompletableFutureを使った並列動画エンコード
- **FFmpeg統合**: ffmpeg-cli-wrapperを使用したFFmpeg操作
- **アップスケーリング検出**: 元動画より大きい解像度への変換を自動検出・拒否

## 📋 移植内容

### 🔄 Rust → Java 変換対応表

| Rust | Java | 実装方法 |
|------|------|----------|
| `Result<T, E>` | `Result<T, E>` | sealed interface + record |
| `struct` | `record` | Java 21 record |
| `impl` | interface/class | 通常のJavaクラス |
| `anyhow` | throws宣言 | Javaの例外処理 |
| `indicatif` | `System.out` | プレーンなコンソール出力 |
| `tokio::spawn` | `CompletableFuture` | Java並列処理 |
| `ffmpeg-sidecar` | `ffmpeg-cli-wrapper` | FFmpegライブラリ |

### 📁 プロジェクト構造

```
src/wappon28dev/vvcnv_java/
├── Main.java                    # メインエントリーポイント
├── util/
│   └── Result.java             # Rust Result型の実装
├── modules/
│   ├── FileModule.java         # ファイル操作（file.rs移植）
│   ├── VideoRes.java           # 動画解像度enum
│   ├── VideoStat.java          # 動画統計情報
│   ├── VideoConfig.java        # 動画設定
│   └── VideoModule.java        # 動画処理（video.rs移植）
└── test/
    └── SimpleVideoTest.java    # 動作検証テスト
```

## 🔧 依存関係

- Java 21+
- Maven
- FFmpeg (システムにインストール済み)
- `net.bramp.ffmpeg:ffmpeg:0.8.0`

## 🚦 実行方法

### メイン機能実行

```bash
mvn clean compile exec:java -Dexec.mainClass="wappon28dev.vvcnv_java.Main"
```

### テスト実行

```bash
mvn clean compile exec:java -Dexec.mainClass="wappon28dev.vvcnv_java.test.SimpleVideoTest"
```

## 📊 動作結果

### 入力

- `assets/01.mp4` (1920x1080, 30fps, 8.4MB)

### 出力例

```
Original video size: 8.4 MB
Video info: 1920x1080 @ 30.0fps
Duration: 00:00:04
Audio streams: 1

✓ All encoding tasks completed!
Success: 10/16
```

### 生成ファイル例

- `01--res-426x240--fps-30--crf-20.mp4` (490KB)
- `01--res-640x360--fps-30--crf-40.mp4` (164KB)
- `01--res-1280x720--fps-30--crf-20.mp4` (3.7MB)
- `01--res-1920x1080--fps-30--crf-40.mp4` (512KB)

## ✨ Java 21機能の活用

### Sealed Class (Result型)

```java
public sealed interface Result<T, E> permits Result.Ok, Result.Err {
    record Ok<T, E>(T value) implements Result<T, E> {}
    record Err<T, E>(E error) implements Result<T, E> {}
}
```

### Pattern Matching

```java
return switch (this) {
    case Ok<T, E>(var value) -> value;
    case Err<T, E>(var error) -> throw new RuntimeException("Unwrapped an Err: " + error);
};
```

### Record Classes

```java
public record VideoConfig(VideoRes res, int fps, int crf, boolean hasAudio) {
    // コンパクトで immutable
}
```

## 🎯 移植時の工夫

1. **Result型の完全実装**: RustのResult APIをJavaで忠実に再現
2. **アップスケーリング検出**: 品質劣化を防ぐ自動チェック
3. **並列処理**: Java 21のCompletableFutureで高速化
4. **エラーハンドリング**: Rustのanyhow風例外処理
5. **型安全性**: recordとsealedで厳密な型チェック

## 🔍 テスト項目

- [x] Result型の動作検証
- [x] ファイル操作テスト
- [x] 動画解像度変換テスト
- [x] 動画統計情報取得
- [x] 動画設定検証
- [x] アップスケーリング検出
- [x] 並列エンコード処理

## 📝 TODO / 制限事項

- [ ] カスタム解像度サポート（現在は予定解像度のみ）
- [ ] より詳細なプログレス表示
- [ ] SLF4Jロガー設定
- [ ] コマンドライン引数サポート
- [ ] より高度なFFmpegオプション

## 🤝 元プロジェクト

このプロジェクトは `vvcnv-rs/` ディレクトリのRustコードを基に移植されました。
元のRustプロジェクトの設計思想と機能を可能な限り忠実に再現しています。
