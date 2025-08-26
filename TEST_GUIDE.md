# テストケース実行ガイド

## 概要
音声認識照合ツール（Voice Validation Tool）のテストケースが作成されました。このドキュメントでは、各種テストの実行方法と内容について説明します。

## テストの種類

### 1. Python単体テスト
**ファイル**: `test_recognize_and_compare.py`

#### テスト内容:
- 類似度計算の正確性テスト
- 差分取得機能のテスト
- 各音声認識エンジン（Google, Whisper, Sphinx）のモックテスト
- タスク処理機能のテスト
- 異常ケース処理のテスト

#### 実行方法:
```bash
python test_recognize_and_compare.py
```

### 2. Python統合テスト
**ファイル**: `test_integration.py`

#### テスト内容:
- 実際のPythonスクリプト実行テスト
- パフォーマンステスト（類似度計算、差分計算の処理速度）
- 実用的なシナリオテスト（完全一致、部分一致、大幅相違など）

#### 実行方法:
```bash
python test_integration.py
```

### 3. Java単体テスト
**ファイル**: `src/test/java/kotothing/voicevalidator/`

#### テスト内容:
- **MainViewControllerTest.java**: UIコントローラーの機能テスト
  - CSVファイル読み込み機能
  - 音声フォルダ読み込み機能
  - エラーハンドリング
  
- **DataClassesTest.java**: データクラスのテスト
  - JSONシリアライゼーション/デシリアライゼーション
  - データ整合性チェック
  - 計算機能の検証

#### 実行方法:
```bash
./gradlew test
```

## テストデータ

### CSVテストデータ
**ファイル**: `test_data/test_script.csv`
- 8種類の難易度別テストケース
- 簡単なフレーズから複雑な文章まで
- 数字、記号、多言語混在テキストを含む

### JSONテストデータ
**ファイル**: `test_data/test_batch_input.json`
- バッチ処理用のテストタスク定義
- 複数の音声ファイルと台本の組み合わせ

## 自動テスト実行

### Windows環境
```cmd
run_tests.bat
```

このバッチファイルが以下を実行します：
1. Python環境の確認
2. 必要ライブラリの確認
3. Python単体テストの実行
4. Python統合テストの実行
5. 結果のサマリー表示

### Java/Gradleテスト
```bash
./gradlew clean test
```

## テストシナリオ詳細

### 1. 類似度計算テスト
- **完全一致**: 類似度 1.0
- **軽微な違い**: 類似度 0.9以上
- **句読点の違い**: 類似度 0.8以上
- **送り仮名の違い**: 類似度 0.7以上
- **大幅な違い**: 類似度 0.3以下

### 2. パフォーマンステスト
- 長文（100回繰り返し）の類似度計算: 1秒以内
- 中程度の文章（50回繰り返し）の差分計算: 0.5秒以内

### 3. エラーハンドリングテスト
- 存在しないファイルの処理
- 不正なJSONフォーマットの処理
- 音声認識APIエラーの処理
- ネットワークエラーの処理

## 継続的インテグレーション

### GitHub Actions設定例
```yaml
name: Voice Validator Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.13'
    - name: Install dependencies
      run: pip install -r requirements.txt
    - name: Run Python tests
      run: |
        python test_recognize_and_compare.py
        python test_integration.py
    - name: Set up Java
      uses: actions/setup-java@v3
      with:
        java-version: '11'
    - name: Run Java tests
      run: ./gradlew test
```

## テスト結果の解釈

### 成功基準
- 全てのunit testがPASS
- パフォーマンステストが基準時間内に完了
- 類似度計算が期待値の範囲内

### 失敗時の対応
1. **ライブラリ不足**: `pip install -r requirements.txt`
2. **音声ファイル不足**: テスト用音声ファイルの作成
3. **パフォーマンス低下**: アルゴリズムの最適化検討

## カバレッジ測定

### Python
```bash
pip install coverage
coverage run test_recognize_and_compare.py
coverage report
coverage html
```

### Java
```bash
./gradlew jacocoTestReport
```

## 今後の拡張予定

1. **E2Eテスト**: 実際の音声ファイルを使った完全なワークフローテスト
2. **負荷テスト**: 大量ファイル処理時のメモリ使用量とパフォーマンス
3. **UIテスト**: JavaFXアプリケーションの操作テスト
4. **多言語テスト**: 日本語以外の言語での認識精度テスト

このテストスイートにより、音声認識照合ツールの品質と信頼性を継続的に確保できます。
