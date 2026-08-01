#!/bin/bash
echo "Starting Voice Validation Tool..."

# Add FFmpeg path if it exists locally
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
FFMPEG_BIN="$DIR/ffmpeg/bin"
if [ -d "$FFMPEG_BIN" ]; then
    export PATH="$FFMPEG_BIN:$PATH"
fi

# Activate Python virtual environment if it exists
if [ -f "venv/bin/activate" ]; then
    echo "Activating Python virtual environment..."
    source venv/bin/activate
else
    echo "Note: Python virtual environment not found. Using system Python."
fi

# Start Java application
echo "Starting application..."
if [ -f "bin/VoiceValidator" ]; then
    bash bin/VoiceValidator
else
    echo "Error: bin/VoiceValidator not found."
    echo "Please ensure the distribution files have been extracted correctly."
    exit 1
fi
