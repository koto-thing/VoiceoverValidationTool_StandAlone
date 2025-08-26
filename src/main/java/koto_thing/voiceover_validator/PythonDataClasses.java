package koto_thing.voiceover_validator;

import java.util.List;

/**
 * 単一の処理タスクを表すクラス
 */
class ProcessingTask {
    public String id;
    public String audioPath;
    public String scriptText;
    
    public ProcessingTask() {}
    
    public ProcessingTask(String id, String audioPath, String scriptText) {
        this.id = id;
        this.audioPath = audioPath;
        this.scriptText = scriptText;
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public String getScriptText() { return scriptText; }
    public void setScriptText(String scriptText) { this.scriptText = scriptText; }
}

/**
 * タスクリストをJSON化するためのラッパークラス
 */
class TaskListWrapper {
    public List<ProcessingTask> tasks;
    
    public TaskListWrapper() {}
    
    public TaskListWrapper(List<ProcessingTask> tasks) {
        this.tasks = tasks;
    }
}

/**
 * Pythonからの単一結果を受け取るクラス
 */
class PythonResult {
    public String id;
    public double similarity;
    public String script_text;
    public String recognized_text;
    public String[] diff;
    public String error;
    
    public PythonResult() {}
}

/**
 * Pythonからのバッチ結果を受け取るクラス
 */
class BatchResult {
    public List<PythonResult> results;
    
    public BatchResult() {}
}