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
        process_task,
        main
    )
except ImportError:
    print("RecognizeAndCompare.pyが見つかりません。")
    sys.exit(1)


class TestCoreFeatures(unittest.TestCase):
    """核心機能のテストケース"""
    
    def setUp(self):
        """テストケースの前処理"""
        self.test_text1 = "こんにちは、これはテストです。"
        self.test_text2 = "こんにちは、これはテストだ。"
        
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
            
    def test_process_task_with_mock_recognition(self):
        """タスク処理のテスト（モック使用）"""
        task = {
            "id": "test001",
            "audioPath": "test_audio.wav",
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

    def test_process_task_with_error(self):
        """エラー処理のテスト"""
        task = {
            "id": "test002",
            "audioPath": "nonexistent.wav",
            "scriptText": "テストテキスト"
        }
        
        # 音声認識でエラーが発生することをシミュレート
        with patch('RecognizeAndCompare.recognize_audio_google', side_effect=Exception("認識エラー")):
            result = process_task(task, "google", "ja-JP", "none")
            
            self.assertEqual(result["id"], "test002")
            self.assertEqual(result["similarity"], 0.0)
            self.assertIsNotNone(result["error"])


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

    def test_numeric_text_scenario(self):
        """数字を含むテキストのシナリオテスト"""
        script = "値段は1500円です"
        recognized = "値段は千五百円です"
        
        similarity = calculate_similarity(script, recognized)
        # 数字と漢数字の違いは中程度の類似度
        self.assertGreater(similarity, 0.4)
        self.assertLess(similarity, 0.8)

    def test_english_mixed_scenario(self):
        """英語混在テキストのシナリオテスト"""
        script = "Hello World プログラム"
        recognized = "ハローワールド プログラム"
        
        similarity = calculate_similarity(script, recognized)
        # 英語とカタカナの違いは中程度の類似度
        self.assertGreater(similarity, 0.3)


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

    def test_batch_processing_simulation(self):
        """バッチ処理のシミュレーションテスト"""
        tasks = [
            {"id": "001", "audioPath": "audio1.wav", "scriptText": "こんにちは"},
            {"id": "002", "audioPath": "audio2.wav", "scriptText": "さようなら"},
            {"id": "003", "audioPath": "audio3.wav", "scriptText": "ありがとう"}
        ]
        
        # 各タスクを処理してみる（音声認識をモック化）
        with patch('RecognizeAndCompare.recognize_audio_google') as mock_recognition:
            mock_recognition.side_effect = ["こんにちは", "さようなら", "ありがとうございます"]
            
            results = []
            for task in tasks:
                result = process_task(task, "google", "ja-JP", "none")
                results.append(result)
            
            # 結果の検証
            self.assertEqual(len(results), 3)
            self.assertEqual(results[0]["similarity"], 1.0)  # 完全一致
            self.assertEqual(results[1]["similarity"], 1.0)  # 完全一致
            self.assertLess(results[2]["similarity"], 1.0)   # 部分一致


def run_focused_tests():
    """重要なテストのみを実行"""
    print("=== 音声認識照合ツール 重点テストスイート ===")
    
    # テストローダーを作成
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # 各テストクラスを追加
    suite.addTests(loader.loadTestsFromTestCase(TestCoreFeatures))
    suite.addTests(loader.loadTestsFromTestCase(TestValidationScenarios))
    suite.addTests(loader.loadTestsFromTestCase(TestIntegration))
    
    # テストを実行
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    # 結果のサマリーを出力
    print(f"\n=== テスト結果サマリー ===")
    print(f"実行: {result.testsRun}")
    print(f"成功: {result.testsRun - len(result.failures) - len(result.errors)}")
    print(f"失敗: {len(result.failures)}")
    print(f"エラー: {len(result.errors)}")
    
    success_rate = (result.testsRun - len(result.failures) - len(result.errors)) / result.testsRun * 100
    print(f"成功率: {success_rate:.1f}%")
    
    return result.wasSuccessful()


if __name__ == '__main__':
    success = run_focused_tests()
    sys.exit(0 if success else 1)
