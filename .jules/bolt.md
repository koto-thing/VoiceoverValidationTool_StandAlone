# Bolt's Journal

## 2024-05-31 - Expensive Model Loading in Iteration
**Learning:** Found that `whisper.load_model(model_name)` was being called inside `recognize_audio_whisper` for every single task being processed in a loop. Loading ML models repeatedly per task rather than initializing once per process dramatically slows down batch processing.
**Action:** Always check if heavy resource loading (like ML models or database connections) can be pulled out of loops or cached globally when processing batches of tasks.

## 2024-06-01 - Hidden O(N²) in JavaFX Controller
**Learning:** Found an O(N²) loop nested inside the UI thread in `MainViewController.java` during `prepareTasks()`. The loop searched for script texts row-by-row in a `List<String[]>` for every single mapped audio file. In large projects, this nested iteration blocking the UI thread could cause noticeable freezing.
**Action:** Always replace `List` lookups with `HashMap` lookups when matching two datasets in a loop.
