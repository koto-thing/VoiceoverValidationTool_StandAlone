package koto_thing.voiceover_validator;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * 音声ファイルとCSVのIDを紐付けるためのデータクラス
 */
public class AudioMapping {
    private final StringProperty fileName; // 音声ファイル名
    private final StringProperty csvId;       // CSVのID
    
    /**
     * コンストラクタ
     */
    public AudioMapping() {
        this("", "未選択");
    }
    
    /**
     * コンストラクタ
     * @param fileName 音声ファイル名
     * @param csvId CSVのID
     */
    public AudioMapping(String fileName, String csvId) {
        this.fileName = new SimpleStringProperty(fileName);
        this.csvId = new SimpleStringProperty(csvId);
    }
    
    // ファイル名のプロパティ
    public StringProperty fileNameProperty() {
        return fileName;
    }
    
    public String getFileName() {
        return fileName.get();
    }
    
    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }
    
    // CSV IDのプロパティ
    public StringProperty csvIdProperty() {
        return csvId;
    }
    
    public String getCsvId() {
        return csvId.get();
    }
    
    public void setCsvId(String csvId) {
        this.csvId.set(csvId);
    }
    
    @Override
    public String toString() {
        return String.format("AudioMapping{fileName='%s', csvId='%s'}", getFileName(), getCsvId());
    }
}
