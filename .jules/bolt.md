# Bolt's Journal

## 2024-05-31 - Expensive Model Loading in Iteration
**Learning:** Found that `whisper.load_model(model_name)` was being called inside `recognize_audio_whisper` for every single task being processed in a loop. Loading ML models repeatedly per task rather than initializing once per process dramatically slows down batch processing.
**Action:** Always check if heavy resource loading (like ML models or database connections) can be pulled out of loops or cached globally when processing batches of tasks.

## 2024-11-23 - MainViewController.java prepareTasks O(N*M) loop
**Learning:** The `prepareTasks` method was taking O(N*M) to map audio files to scripts in the CSV. Since N can be hundreds of audio files and M is the CSV size, this caused frontend unresponsiveness and slow task generation.
**Action:** Replaced linear searches (O(N*M)) inside loops with HashMap caching (O(N+M)) before processing loops, particularly when dealing with mapped collections like UI to file selections.
