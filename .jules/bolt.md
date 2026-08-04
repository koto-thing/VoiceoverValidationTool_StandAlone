# Bolt's Journal

## 2024-05-31 - Expensive Model Loading in Iteration
**Learning:** Found that `whisper.load_model(model_name)` was being called inside `recognize_audio_whisper` for every single task being processed in a loop. Loading ML models repeatedly per task rather than initializing once per process dramatically slows down batch processing.
**Action:** Always check if heavy resource loading (like ML models or database connections) can be pulled out of loops or cached globally when processing batches of tasks.

## 2024-05-31 - Expensive UI Re-renders in JavaFX
**Learning:** Found that modifying an `ObservableList` (`results.add()`) inside a loop causes O(N) change events and UI re-renders, dramatically slowing down list updates when processing multiple items.
**Action:** Always use a temporary list (like `ArrayList`) to collect items during loop iterations, then apply a single `results.addAll(tempList)` update to batch change events and minimize UI freezing.
