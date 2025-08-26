package koto_thing.voiceover_validator;

import javafx.beans.property.*;

/**
 * 音声認識の検証結果を表すクラス
 */
public class ValidationResult {
    private final StringProperty id;
    private final DoubleProperty similarity;
    private final StringProperty scriptText;
    private final StringProperty recognizedText;
    private final StringProperty status;
    
    public ValidationResult() {
        this("", 0.0, "", "", "");
    }
    
    public ValidationResult(String id, double similarity, String scriptText, String recognizedText, String status) {
        this.id = new SimpleStringProperty(id);
        this.similarity = new SimpleDoubleProperty(similarity);
        this.scriptText = new SimpleStringProperty(scriptText);
        this.recognizedText = new SimpleStringProperty(recognizedText);
        this.status = new SimpleStringProperty(status);
    }
    
    // IDのプロパティ
    public StringProperty idProperty() {
        return id;
    }
    
    public String getId() {
        return id.get();
    }
    
    public void setId(String id) {
        this.id.set(id);
    }
    
    // 類似度のプロパティ
    public DoubleProperty similarityProperty() {
        return similarity;
    }
    
    public double getSimilarity() {
        return similarity.get();
    }
    
    public void setSimilarity(double similarity) {
        this.similarity.set(similarity);
    }
    
    // セリフテキストのプロパティ
    public StringProperty scriptTextProperty() {
        return scriptText;
    }
    
    public String getScriptText() {
        return scriptText.get();
    }
    
    public void setScriptText(String scriptText) {
        this.scriptText.set(scriptText);
    }
    
    // 認識されたテキストのプロパティ
    public StringProperty recognizedTextProperty() {
        return recognizedText;
    }
    
    public String getRecognizedText() {
        return recognizedText.get();
    }
    
    public void setRecognizedText(String recognizedText) {
        this.recognizedText.set(recognizedText);
    }
    
    // ステータスのプロパティ
    public StringProperty statusProperty() {
        return status;
    }
    
    public String getStatus() {
        return status.get();
    }
    
    public void setStatus(String status) {
        this.status.set(status);
    }
    
    @Override
    public String toString() {
        return String.format("ValidationResult{id='%s', similarity=%.3f, status='%s'}", 
                           getId(), getSimilarity(), getStatus());
    }
}
