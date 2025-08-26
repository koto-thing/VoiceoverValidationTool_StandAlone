@echo off
echo ========================================
echo 音声認識照合ツール テストスイート実行
echo ========================================

echo.
echo [1/4] Python環境の確認中...
python --version
if %errorlevel% neq 0 (
    echo エラー: Pythonが見つかりません
    pause
    exit /b 1
)

echo.
echo [2/4] 必要なライブラリの確認中...
python -c "import speech_recognition, whisper, difflib" 2>nul
if %errorlevel% neq 0 (
    echo 警告: 一部のライブラリが見つかりません（音声認識テストはスキップされます）
)

echo.
echo [3/4] 単体テストの実行...
python test_recognize_and_compare.py
if %errorlevel% neq 0 (
    echo エラー: 単体テストで失敗が発生しました
    set UNIT_TEST_FAILED=1
)

echo.
echo [4/4] 統合テストの実行...
python test_integration.py
if %errorlevel% neq 0 (
    echo エラー: 統合テストで失敗が発生しました
    set INTEGRATION_TEST_FAILED=1
)

echo.
echo ========================================
echo テスト実行完了
echo ========================================

if defined UNIT_TEST_FAILED (
    echo 単体テストで失敗がありました
)
if defined INTEGRATION_TEST_FAILED (
    echo 統合テストで失敗がありました
)

if not defined UNIT_TEST_FAILED if not defined INTEGRATION_TEST_FAILED (
    echo 全てのテストが正常に完了しました！
)

echo.
pause
