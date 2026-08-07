## 2024-05-24 - O(N^2) Lookup in UI Thread
**Learning:** The JavaFX UI thread was handling an O(N*M) nested loop operation (`prepareTasks` calling `findScriptTextById`) to map CSV scripts to audio files. In a project bridging a UI orchestration layer and heavy external processing (Python), keeping the UI responsive during prep operations is critical.
**Action:** When iterating over collections on the UI thread to construct task payloads, always use O(1) structures (like HashMaps) for cross-referencing datasets rather than linear array searches.
