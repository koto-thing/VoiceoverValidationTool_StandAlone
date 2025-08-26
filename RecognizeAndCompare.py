#!/usr/bin/env python
# -*- coding: utf-8 -*-

import sys
import json
import os
import difflib
from typing import List, Dict, Any
import tempfile

# unittest から RecognizeAndCompare.sr / .whisper をパッチできるようにモジュール変数として保持
sr = None  # type: ignore
whisper = None  # type: ignore


def calculate_similarity(text1: str, text2: str) -> float:
    """2つのテキストの類似度を計算"""
    return difflib.SequenceMatcher(None, text1, text2).ratio()


def get_diff(text1: str, text2: str) -> List[str]:
    """2つのテキストの差分を取得"""
    diff = list(difflib.unified_diff(
        text1.splitlines(keepends=True),
        text2.splitlines(keepends=True),
        fromfile='script',
        tofile='recognized',
        lineterm=''
    ))
    return diff


# サポート外形式をWAVに変換（Google/Sphinx用）
def ensure_wav_for_engine(audio_path: str, engine: str) -> str:
    if engine not in ("google", "sphinx"):
        return audio_path
    ext = os.path.splitext(audio_path)[1].lower()
    if ext in (".wav", ".aiff", ".aif", ".flac"):
        return audio_path
    try:
        from pydub import AudioSegment
        # 変換先テンポラリWAV
        fd, tmp_path = tempfile.mkstemp(prefix="vvt_", suffix=".wav")
        os.close(fd)
        seg = AudioSegment.from_file(audio_path)
        seg.export(tmp_path, format="wav")
        return tmp_path
    except Exception as e:
        # FFmpegが見つからない場合やpydubがない場合は元のパスを返す
        print(f"Warning: Could not convert audio file: {e}", file=sys.stderr)
        return audio_path


# クリーニング: 実行後テンポラリを削除できるように記録
_TEMP_FILES: List[str] = []


def recognize_audio_google(audio_path: str, language_code: str) -> str:
    """Google Speech-to-Text APIを使用した音声認識"""
    global sr
    try:
        if sr is None:
            import speech_recognition as _sr  # 遅延インポート
            sr = _sr
        recognizer = sr.Recognizer()
        with sr.AudioFile(audio_path) as source:
            audio = recognizer.record(source)
        return recognizer.recognize_google(audio, language=language_code)
    except ImportError:
        raise Exception("SpeechRecognitionが未インストールです。'pip install -r requirements.txt' を実行してください。")
    except Exception as e:
        raise Exception(f"Google recognition failed: {str(e)}")


def recognize_audio_whisper(audio_path: str, language_code: str, model_name: str) -> str:
    """Whisperを使用した音声認識"""
    global whisper
    try:
        if whisper is None:
            import whisper as _whisper  # 遅延インポート
            whisper = _whisper
        model = whisper.load_model(model_name)
        result = model.transcribe(audio_path, language=language_code)
        return result.get("text", "")
    except ImportError:
        raise Exception("Whisperが未インストールです。'pip install -r requirements.txt' を実行してください（torch含む）。")
    except Exception as e:
        raise Exception(f"Whisper recognition failed: {str(e)}")


def recognize_audio_sphinx(audio_path: str, language_code: str) -> str:
    """Sphinxを使用した音声認識"""
    global sr
    try:
        if sr is None:
            import speech_recognition as _sr  # 遅延インポート
            sr = _sr
        recognizer = sr.Recognizer()
        with sr.AudioFile(audio_path) as source:
            audio = recognizer.record(source)
        return recognizer.recognize_sphinx(audio)
    except ImportError:
        raise Exception("SpeechRecognitionが未インストールです。'pip install -r requirements.txt' を実行してください。")
    except Exception as e:
        raise Exception(f"Sphinx recognition failed: {str(e)}")


def process_task(task: Dict[str, Any], engine: str, language_code: str, whisper_model: str) -> Dict[str, Any]:
    """単一のタスクを処理"""
    try:
        audio_path = task["audioPath"]
        script_text = task["scriptText"]
        task_id = task["id"]

        # Google/Sphinx は WAV 変換を試行（Whisper は元のフォーマットを使う）
        prepped_path = ensure_wav_for_engine(audio_path, engine)
        if prepped_path != audio_path:
            _TEMP_FILES.append(prepped_path)

        if engine == "google":
            recognized_text = recognize_audio_google(prepped_path, language_code)
        elif engine == "whisper":
            recognized_text = recognize_audio_whisper(audio_path, language_code, whisper_model)
        elif engine == "sphinx":
            recognized_text = recognize_audio_sphinx(prepped_path, language_code)
        else:
            raise Exception(f"Unknown engine: {engine}")

        similarity = calculate_similarity(script_text, recognized_text)
        diff = get_diff(script_text, recognized_text)

        return {
            "id": task_id,
            "similarity": similarity,
            "script_text": script_text,
            "recognized_text": recognized_text,
            "diff": diff,
            "error": None
        }
    except Exception as e:
        return {
            "id": task.get("id", "unknown"),
            "similarity": 0.0,
            "script_text": task.get("scriptText", ""),
            "recognized_text": "",
            "diff": [],
            "error": str(e)
        }


def main():
    """メイン関数"""
    if len(sys.argv) != 5:
        print("Usage: python RecognizeAndCompare.py <engine> <language_code> <whisper_model> <json_file>", file=sys.stderr)
        sys.exit(1)

    engine = sys.argv[1]
    language_code = sys.argv[2]
    whisper_model = sys.argv[3]
    json_file = sys.argv[4]

    try:
        # JSONファイルを読み込み（BOM対応）
        with open(json_file, 'r', encoding='utf-8-sig') as f:
            data = json.load(f)

        tasks = data.get("tasks", [])
        results = []

        # 各タスクを処理
        for i, task in enumerate(tasks):
            # 進捗をstderrに出力（UI側で進捗表示用）
            progress = (i + 1) * 100 // len(tasks) if tasks else 100
            print(f"Processing task {i+1}/{len(tasks)} ({progress}%)", file=sys.stderr)

            result = process_task(task, engine, language_code, whisper_model)
            results.append(result)

        # 結果をJSONとして出力
        output = {"results": results}
        print(json.dumps(output, ensure_ascii=False, indent=2))

    except Exception as e:
        # エラー結果を出力
        error_output = {
            "results": [{
                "id": "error",
                "similarity": 0.0,
                "script_text": "",
                "recognized_text": "",
                "diff": [],
                "error": str(e)
            }]
        }
        print(json.dumps(error_output, ensure_ascii=False, indent=2))
        sys.exit(1)
    finally:
        # テンポラリファイルのクリーンアップ
        for temp_file in _TEMP_FILES:
            try:
                if os.path.exists(temp_file):
                    os.unlink(temp_file)
            except Exception:
                pass


if __name__ == "__main__":
    main()
