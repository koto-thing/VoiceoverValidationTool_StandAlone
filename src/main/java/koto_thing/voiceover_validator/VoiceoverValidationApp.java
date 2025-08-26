package koto_thing.voiceover_validator;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.filechooser.FileSystemView;

/**
 * メインアプリケーションクラス
 */
public class VoiceoverValidationApp extends Application {
    
    // 例外ログ
    private static PrintWriter LOG;

    static {
        try {
            File docs = getDocumentsDirectory();
            File dir = new File(docs, "VoiceValidator/logs");
            if (!dir.exists()) dir.mkdirs();
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File f = new File(dir, "app_" + ts + ".log");
            LOG = new PrintWriter(f, StandardCharsets.UTF_8);
            // 終了時にクローズ
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try { if (LOG != null) LOG.close(); } catch (Exception ignored) {}
            }));
        } catch (Exception ignored) {
            LOG = null;
        }
        // 未捕捉例外をログ
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> logError("Uncaught exception in " + t.getName(), e));
    }

    private static File getDocumentsDirectory() {
        try {
            File def = FileSystemView.getFileSystemView().getDefaultDirectory();
            if (def != null && def.exists()) return def;
        } catch (Throwable ignored) { }
        // フォールバック: ユーザーホーム配下の Documents
        File fb = new File(System.getProperty("user.home"), "Documents");
        if (!fb.exists()) fb.mkdirs();
        return fb;
    }

    private static void logError(String msg, Throwable e) {
        try {
            if (LOG != null) {
                LOG.println("[" + LocalDateTime.now() + "] " + msg);
                if (e != null) e.printStackTrace(LOG);
                LOG.flush();
            }
        } catch (Exception ignored) { }
    }
    
    /**
     * アプリケーションのエントリーポイント
     * @param stage メインステージ
     * @throws IOException FXMLの読み込みに失敗した場合
    */
    @Override
    public void start(Stage stage) throws IOException {
        try {
            // FXMLファイルの読み込み
            FXMLLoader fxmlLoader = new FXMLLoader(VoiceoverValidationApp.class.getResource("main-view.fxml"));
            // シーンの作成
            Scene scene = new Scene(fxmlLoader.load());
            
            // CSSスタイルシートの適用
            scene.getStylesheets().add(
                    VoiceoverValidationApp.class.getResource("modern-style.css").toExternalForm()
            );
            
            // ステージの設定
            stage.setTitle("Voice Validation Tool - 音声認識照合ツール");
            stage.setScene(scene);
            
            // アプリケーションのアイコンを設定
            try {
                stage.getIcons().add(new Image(VoiceoverValidationApp.class.getResourceAsStream("icon.png")));
            } catch (Exception e) {
                // Icon file not found, continue without icon
            }
            
            // ウィンドウの最小サイズを設定
            stage.setMinWidth(900);
            stage.setMinHeight(700);
            
            // 初期ウィンドウサイズを画面サイズに基づいて設定
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double windowWidth = Math.min(1200, screenBounds.getWidth() * 0.8);
            double windowHeight = Math.min(800, screenBounds.getHeight() * 0.8);
            
            stage.setWidth(windowWidth);
            stage.setHeight(windowHeight);
            stage.centerOnScreen();
            
            // 最大化はせず、リサイズ可能に設定
            stage.setMaximized(false);
            stage.setResizable(true);
            
            stage.show();
        } catch (Throwable e) {
            logError("Startup failure", e);
            // 例外を再送出してjpackageのコンソールにも表示させる
            if (e instanceof IOException io) throw io;
            if (e instanceof RuntimeException re) throw re;
            throw new RuntimeException(e);
        }
    }
    
    /**
    * メインメソッド - アプリケーションの起動
    * @param args コマンドライン引数
    */
    public static void main(String[] args) {
        launch();
    }
}
