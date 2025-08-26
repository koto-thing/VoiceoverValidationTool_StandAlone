#!/usr/bin/env python
# -*- coding: utf-8 -*-

import unittest
import json
import os
import tempfile
from unittest.mock import patch, MagicMock
import sys

# テスト対象のモジュールをインポート
try:
    from RecognizeAndCompare import (
        calculate_similarity,
        get_diff,
        recognize_audio_google,
        recognize_audio_whisper,
        recognize_audio_sphinx,
        process_task,
        main
    )
except ImportError:
    print("RecognizeAndCompare.pyが見つかりません。")
    sys.exit(1)


class TestRecognizeAndCompare(unittest.TestCase):
    """音声認識と比較機能のテストケース"""
    
    def setUp(self):
        """テストケースの前処理"""
        self.test_text1 = "こんにちは、これはテストです。"
        self.test_text2 = "こんにちは、これはテストだ。"
        self.test_audio_path = "test_audio.wav"
        
    def test_calculate_similarity_identical(self):
        """同一テキストの類似度テスト"""
        similarity = calculate_similarity(self.test_text1, self.test_text1)
        self.assertEqual(similarity, 1.0)
        
    def test_calculate_similarity_different(self):
        """異なるテキストの類似度テスト"""
        similarity = calculate_similarity(self.test_text1, self.test_text2)
        self.assertGreater(similarity, 0.0)
        self.assertLess(similarity, 1.0)
        
    def test_calculate_similarity_completely_different(self):
        """完全に異なるテキストの類似度テスト"""
        similarity = calculate_similarity("hello", "さようなら")
        self.assertLess(similarity, 0.5)
        
    def test_get_diff_identical(self):
        """同一テキストの差分テスト"""
        diff = get_diff(self.test_text1, self.test_text1)
        # 同一の場合、ヘッダー行のみで差分はない
        self.assertEqual(len([line for line in diff if line.startswith('+') or line.startswith('-')]), 0)
        
    def test_get_diff_different(self):
        """異なるテキストの差分テスト"""
        diff = get_diff(self.test_text1, self.test_text2)
        self.assertGreater(len(diff), 0)
    
    @patch('RecognizeAndCompare.sr')  # speech_recognitionモジュール全体をパッチ
    def test_recognize_audio_google_success(self, mock_sr):
        """Google音声認識の成功テスト"""
        # モックの設定
        mock_recognizer = MagicMock()
        mock_sr.Recognizer.return_value = mock_recognizer
        mock_sr.AudioFile.return_value.__enter__.return_value = MagicMock()
        mock_recognizer.record.return_value = MagicMock()
        mock_recognizer.recognize_google.return_value = self.test_text1
        
        # パッチを適用してimportをシミュレート
        with patch.dict('sys.modules', {'speech_recognition': mock_sr}):
            result = recognize_audio_google(self.test_audio_path, "ja-JP")
            self.assertEqual(result, self.test_text1)
        
    @patch('RecognizeAndCompare.sr')  # speech_recognitionモジュール全体をパッチ
    def test_recognize_audio_google_failure(self, mock_sr):
        """Google音声認識の失敗テスト"""
        mock_recognizer = MagicMock()
        mock_sr.Recognizer.return_value = mock_recognizer
        mock_sr.AudioFile.return_value.__enter__.return_value = MagicMock()
        mock_recognizer.record.return_value = MagicMock()
        mock_recognizer.recognize_google.side_effect = Exception("Recognition failed")
        
        with patch.dict('sys.modules', {'speech_recognition': mock_sr}):
            with self.assertRaises(Exception):
                recognize_audio_google(self.test_audio_path, "ja-JP")
            
    @patch('RecognizeAndCompare.whisper')  # whisperモジュール全体をパッチ
    def test_recognize_audio_whisper_success(self, mock_whisper):
        """Whisper音声認識の成功テスト"""
        mock_model = MagicMock()
        mock_whisper.load_model.return_value = mock_model
        mock_model.transcribe.return_value = {"text": self.test_text1}
        
        with patch.dict('sys.modules', {'whisper': mock_whisper}):
            result = recognize_audio_whisper(self.test_audio_path, "ja", "base")
            self.assertEqual(result, self.test_text1)
        
    @patch('RecognizeAndCompare.whisper')  # whisperモジュール全体をパッチ
    def test_recognize_audio_whisper_failure(self, mock_whisper):
        """Whisper音声認識の失敗テスト"""
        mock_whisper.load_model.side_effect = Exception("Model loading failed")
        
        with patch.dict('sys.modules', {'whisper': mock_whisper}):
            with self.assertRaises(Exception):
                recognize_audio_whisper(self.test_audio_path, "ja", "base")
            
    def test_process_task_with_mock_recognition(self):
        """タスク処理のテスト（モック使用）"""
        task = {
            "id": "test001",
            "audioPath": self.test_audio_path,
            "scriptText": self.test_text1
        }
        
        # 音声認識をモック化
        with patch('RecognizeAndCompare.recognize_audio_google', return_value=self.test_text1):
            result = process_task(task, "google", "ja-JP", "none")
            
            self.assertEqual(result["id"], "test001")
            self.assertEqual(result["similarity"], 1.0)
            self.assertEqual(result["script_text"], self.test_text1)
            self.assertEqual(result["recognized_text"], self.test_text1)
            self.assertIsNone(result["error"])


class TestIntegration(unittest.TestCase):
    """統合テストケース"""
    
    def setUp(self):
        """統合テストの前処理"""
        self.temp_dir = tempfile.mkdtemp()
        
    def tearDown(self):
        """統合テストの後処理"""
        import shutil
        shutil.rmtree(self.temp_dir, ignore_errors=True)
        
    def test_main_function_with_sample_json(self):
        """メイン関数のサンプルJSONテスト"""
        # テスト用JSONファイルを作成
        test_data = {
            "tasks": [
                {
                    "id": "test001",
                    "audioPath": "nonexistent_audio.wav",
                    "scriptText": "テストテキスト"
                }
            ]
        }
        
        json_path = os.path.join(self.temp_dir, "test.json")
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump(test_data, f, ensure_ascii=False)
        
        # メイン関数の呼び出し（音声ファイルが存在しないためエラーになることを期待）
        with patch('sys.argv', ['RecognizeAndCompare.py', 'google', 'ja-JP', 'none', json_path]):
            with patch('sys.stdout'):  # 出力をキャプチャ
                try:
                    main()
                except SystemExit:
                    pass  # 正常終了


class TestValidationScenarios(unittest.TestCase):
    """検証シナリオのテストケース"""
    
    def test_high_similarity_scenario(self):
        """高い類似度のシナリオテスト"""
        script = "今日は良い天気ですね。"
        recognized = "今日は良い天気です。"
        
        similarity = calculate_similarity(script, recognized)
        self.assertGreater(similarity, 0.8)  # 80%以上の類似度を期待
        
    def test_low_similarity_scenario(self):
        """低い類似度のシナリオテスト"""
        script = "今日は良い天気ですね。"
        recognized = "昨日は雨でした。"
        
        similarity = calculate_similarity(script, recognized)
        self.assertLess(similarity, 0.5)  # 50%未満の類似度を期待
        
    def test_punctuation_differences(self):
        """句読点の違いによるテスト"""
        script = "こんにちは、元気ですか？"
        recognized = "こんにちは元気ですか"
        
        similarity = calculate_similarity(script, recognized)
        self.assertGreater(similarity, 0.7)  # 句読点の違いは許容範囲
        
    def test_katakana_hiragana_differences(self):
        """ひらがな・カタカナの違いのテスト"""
        script = "コンピュータ"
        recognized = "こんぴゅーた"
        
        similarity = calculate_similarity(script, recognized)
        # 音が同じでも文字が違うため、類似度は低めに調整
        self.assertGreater(similarity, 0.1)  # 最低限の類似度は確保
        self.assertLess(similarity, 0.5)     # しかし高すぎない


if __name__ == '__main__':
    # テストの実行
    unittest.main(verbosity=2)
