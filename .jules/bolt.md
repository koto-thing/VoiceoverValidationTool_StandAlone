# Bolt's Journal

## 2024-05-31 - Expensive Model Loading in Iteration
**Learning:** Found that `whisper.load_model(model_name)` was being called inside `recognize_audio_whisper` for every single task being processed in a loop. Loading ML models repeatedly per task rather than initializing once per process dramatically slows down batch processing.
**Action:** Always check if heavy resource loading (like ML models or database connections) can be pulled out of loops or cached globally when processing batches of tasks.
## 2024-05-18 - [Optimize Nested Loop Matching in UI Controller]
**Learning:** In `MainViewController.java`, task preparation iterated over Audio Mappings and linearly searched CSV data arrays (O(N*M)). For large CSV lists and many audio files, this scales poorly and blocks the JavaFX application thread.
**Action:** Use a `HashMap` to index script data by `csvId` in a single pass O(M), then look up items in O(1) time per audio mapping O(N), bringing total time complexity to O(N+M) and keeping the UI thread fast.
