import os
import sys

# FFmpegのパスを確認
ffmpeg_path = r"D:\Pandd\VoiceValidationTool\VoiceValidator\ffmpeg\bin\ffmpeg.exe"

if os.path.exists(ffmpeg_path):
    print(f"SUCCESS: FFmpeg found at {ffmpeg_path}")
    # 環境変数に追加
    bin_dir = os.path.dirname(ffmpeg_path)
    if bin_dir not in os.environ.get("PATH", ""):
        os.environ["PATH"] = bin_dir + os.pathsep + os.environ.get("PATH", "")
        print(f"Added to PATH: {bin_dir}")
    
    # pydubの設定
    try:
        from pydub import AudioSegment
        AudioSegment.converter = ffmpeg_path
        AudioSegment.ffprobe = os.path.join(bin_dir, "ffprobe.exe")
        print("pydub configured successfully")
    except ImportError:
        print("pydub not available")
    
    print("FFmpeg setup complete")
else:
    print(f"ERROR: FFmpeg not found at {ffmpeg_path}")
    sys.exit(1)
