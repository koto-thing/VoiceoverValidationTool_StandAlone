package koto_thing.voiceover_validator;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.TableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import com.google.gson.Gson;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.SnapshotParameters;
import javafx.scene.layout.StackPane;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ScrollBar;
import javafx.geometry.Orientation;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ComboBox;
import javafx.scene.text.Text;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;

/**
 * メインビューのコントローラクラス
 */
public class MainViewController implements Initializable {
    // 設定関連
    @FXML private TextField pythonPathField;
    @FXML private ComboBox<String> engineComboBox;
    @FXML private TextField languageCodeField;
    @FXML private Label whisperModelLabel;
    @FXML private ComboBox<String> whisperModelComboBox;

    // 入力関連
    @FXML private TextField scriptPathField;
    @FXML private TextField audioFolderPathField;
    @FXML private TextField columnNameField;

    // プレビューとマッピング
    @FXML private VBox csvPreviewCard;
    @FXML private TableView<ObservableList<String>> csvPreviewTable;
    @FXML private VBox audioMappingCard;
    @FXML private TableView<AudioMapping> audioMappingTable;
    @FXML private TableColumn<AudioMapping, String> fileNameColumn;
    @FXML private TableColumn<AudioMapping, String> csvIdColumn;

    // アクションボタン
    @FXML private Button validateButton;
    @FXML private Button clearResultsButton;
    @FXML private Button exportResultsButton;

    // 結果表示関連
    @FXML private VBox resultsCard;
    @FXML private VBox progressContainer;
    @FXML private Label progressLabel;
    @FXML private ProgressBar progressBar;
    @FXML private TableView<ValidationResult> resultTable;
    @FXML private TableColumn<ValidationResult, String> idColumn;
    @FXML private TableColumn<ValidationResult, Double> similarityColumn;
    @FXML private TableColumn<ValidationResult, String> scriptColumn;
    @FXML private TableColumn<ValidationResult, String> recognizedColumn;
    @FXML private TableColumn<ValidationResult, String> statusColumn;

    // ステータスバー
    @FXML private Label statusLabel;
    @FXML private Label versionLabel;
    @FXML private ToggleButton themeToggle;
    @FXML private FontIcon themeIcon;

    // タイトルのラベルとアイコン
    @FXML private Label titleLabel;
    @FXML private FontIcon titleIcon;

    // データモデル
    private final ObservableList<String> csvIds = FXCollections.observableArrayList();
    private final List<String[]> csvData = new ArrayList<>();
    private final ObservableList<ValidationResult> results = FXCollections.observableArrayList();
    private final ObservableList<AudioMapping> audioMappings = FXCollections.observableArrayList();

    // 現在の処理タスク
    private Task<Void> currentTask;

    // 設定
    private final Preferences prefs = Preferences.userNodeForPackage(MainViewController.class);

    private boolean themeInitialized = false;

    // 中央のスクロールペイン
    @FXML private ScrollPane mainScroll;

    /**
     * 初期化処理
     * @param location FXMLの場所
     * @param resources リソースバンドル
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        setupEventHandlers();
        loadDefaultSettings();
        restoreTheme();
        Platform.runLater(this::attachHoverAnimationToButtons);
        sanitizeTitle();
        fallbackTitleIcon();
        updateStatus("準備完了", false);
    }

    /**
     * ボタンにホバーアニメーションを適用
     */
    private void attachHoverAnimationToButtons() {
        Scene scene = statusLabel != null ? statusLabel.getScene() : null;
        if (scene == null) return;
        Parent root = scene.getRoot();
        Set<Node> targets = new HashSet<>();
        targets.addAll(root.lookupAll(".button"));
        targets.addAll(root.lookupAll(".toggle-button"));
        for (Node n : targets) {
            if (n instanceof ButtonBase btn) {
                setupHoverAnimation(btn);
            }
        }
    }

    /**
     * ホバーアニメーションのセットアップ
     * @param btn ホバーアニメーションを適用するボタン
     */
    private void setupHoverAnimation(ButtonBase btn) {
        btn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> animateTranslate(btn, -2));
        btn.addEventHandler(MouseEvent.MOUSE_EXITED, e -> animateTranslate(btn, 0));
        btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> animateTranslate(btn, 0));
        btn.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (btn.isHover()) 
                animateTranslate(btn, -2); 
            else 
                animateTranslate(btn, 0);
        });
    }

    /**
     * ノードをY方向にアニメーションで移動
     * @param node 移動するノード
     * @param toY 目的のY座標（相対）
     */
    private void animateTranslate(Node node, double toY) {
        Object key = node.getProperties().get("hoverTT");
        TranslateTransition tt;
        
        if (key instanceof TranslateTransition existing) {
            tt = existing;
            tt.stop();
        } else {
            tt = new TranslateTransition(Duration.millis(140), node);
            tt.setInterpolator(Interpolator.EASE_BOTH);
            node.getProperties().put("hoverTT", tt);
        }
        
        tt.setFromY(node.getTranslateY());
        tt.setToY(toY);
        tt.playFromStart();
    }

    /**
     * UIのセットアップ
     */
    private void setupUI() {
        // エンジンのオプション
        engineComboBox.setItems(FXCollections.observableArrayList("google", "whisper", "sphinx"));
        engineComboBox.setValue("google");

        // Whisperのモデル
        whisperModelComboBox.setItems(FXCollections.observableArrayList("tiny", "base", "small", "medium", "large"));
        whisperModelComboBox.setValue("base");

        // デフォルト値
        languageCodeField.setText("ja-JP");
        columnNameField.setText("context");

        // テーブル
        setupAudioMappingTable();
        setupResultsTable();
        
        // テーブルの初期高さ設定
        setupTableHeights();

        // カードを非表示
        csvPreviewCard.setVisible(false);
        audioMappingCard.setVisible(false);
        resultsCard.setVisible(false);
        progressContainer.setVisible(false);

        updateWhisperModelVisibility();
    }

    /**
     * イベントハンドラのセットアップ
     */
    private void setupEventHandlers() {
        // エンジン選択変更時の処理
        engineComboBox.setOnAction(e -> {
            updateLanguageCodeForEngine();
            updateWhisperModelVisibility();
        });

        // ファイルパス変更時の処理
        scriptPathField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isEmpty() && !n.equals(o)) loadCsvFile(n);
        });
        
        // 音声フォルダパス変更時の処理
        audioFolderPathField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isEmpty() && !n.equals(o)) loadAudioFolder(n);
        });
    }

    /**
     * オーディオマッピングテーブルのセットアップ
     */
    private void setupAudioMappingTable() {
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        csvIdColumn.setCellValueFactory(new PropertyValueFactory<>("csvId"));

        // 固定セル高を解除(可変行高にする)
        audioMappingTable.setFixedCellSize(Region.USE_COMPUTED_SIZE);

        // 長文列は折り返し表示+可変行高
        fileNameColumn.setCellFactory(col -> new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(16));
            }
            // アイテム更新時にテキストを設定
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setPrefHeight(Region.USE_COMPUTED_SIZE);
                }
            }
        });

        // 常時ComboBoxを表示してクリック可能であることを明示
        csvIdColumn.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>(csvIds);
            {
                combo.setMaxWidth(Double.MAX_VALUE);
                combo.setPromptText("未選択");
                combo.getStyleClass().add("mapping-combo");
                combo.setOnShowing(e -> rememberScrollPositions());
                combo.setOnAction(e -> {
                    AudioMapping rowItem = getTableRow() != null ? getTableRow().getItem() : null;
                    if (rowItem != null) {
                        String newVal = combo.getValue();
                        preserveScrollBoth(audioMappingTable, mainScroll, () -> rowItem.setCsvId(newVal));
                    }
                });
            }
            // アイテム更新時にComboBoxの選択を同期
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    if (item == null || item.isBlank()) combo.getSelectionModel().clearSelection();
                    else combo.getSelectionModel().select(item);
                    setGraphic(combo);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setPrefHeight(Region.USE_COMPUTED_SIZE);
                }
            }
        });

        // データバインディング
        audioMappingTable.setItems(audioMappings);
        audioMappingTable.setEditable(true);
        
        // テーブルの高さを可変にする設定
        audioMappingTable.setPrefHeight(Region.USE_COMPUTED_SIZE);
        audioMappingTable.setMaxHeight(Double.MAX_VALUE);
    }

    // テーブルの高さを可変にする設定
    private Double lastTableScroll = null;
    private Double lastPaneScroll = null;
    private void rememberScrollPositions() {
        ScrollBar vsb = findVerticalScrollBar(audioMappingTable);
        lastTableScroll = (vsb != null) ? vsb.getValue() : null;
        lastPaneScroll = (mainScroll != null) ? mainScroll.getVvalue() : null;
    }

    /**
     * スクロール位置を保持したままテーブルを更新するユーティリティ
     * @param table 更新対象のTableView
     * @param container 外側のScrollPane
     * @param mutate 更新処理を行うRunnable
     */
    private void preserveScrollBoth(TableView<?> table, ScrollPane container, Runnable mutate) {
        ScrollBar vsb = findVerticalScrollBar(table);
        Double tableVal = (vsb != null) ? vsb.getValue() : lastTableScroll;
        Double paneVal = (container != null) ? container.getVvalue() : lastPaneScroll;
        mutate.run();
        
        // 次のtickでスクロール位置を復元
        Platform.runLater(() -> {
            ScrollBar again = findVerticalScrollBar(table);
            if (again != null && tableVal != null) again.setValue(tableVal);
            if (container != null && paneVal != null) container.setVvalue(paneVal);
        });
    }
    
    /**
     * スクロール位置を保持したままテーブルを更新するユーティリティ
     * @param table 更新対象のTableView
     * @param mutate 更新処理を行うRunnable
     */
    private void preserveScroll(TableView<?> table, Runnable mutate) {
        ScrollBar vsb = findVerticalScrollBar(table);
        Double val = (vsb != null) ? vsb.getValue() : null;
        mutate.run();
        
        // 次のtickでスクロール位置を復元
        if (val != null) {
            Platform.runLater(() -> {
                ScrollBar again = findVerticalScrollBar(table);
                if (again != null) again.setValue(val);
            });
        }
    }

    /**
     * TableViewから垂直スクロールバーを探すユーティリティ
     * @param table 対象のTableView
     * @return 見つかったScrollBar、見つからなければnull
     */
    private ScrollBar findVerticalScrollBar(TableView<?> table) {
        if (table.getSkin() == null) {
            table.applyCss();
            table.layout();
        }
        
        // TableViewの子ノードからスクロールバーを探す
        for (var node : table.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar sb && sb.getOrientation() == Orientation.VERTICAL) {
                return sb;
            }
        }
        
        return null;
    }

    /**
     * 検証結果テーブルのセットアップ
     */
    private void setupResultsTable() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        scriptColumn.setCellValueFactory(new PropertyValueFactory<>("scriptText"));
        recognizedColumn.setCellValueFactory(new PropertyValueFactory<>("recognizedText"));
        similarityColumn.setCellValueFactory(new PropertyValueFactory<>("similarity"));
        similarityColumn.setCellFactory(col -> new TableCell<>() {
            // 色分けしてパーセント表示
            @Override protected void updateItem(Double s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { 
                    setText(null); 
                    setStyle(""); 
                    return; 
                }
                
                setText(String.format(Locale.US, "%.1f%%", s * 100));
                if (s >= 0.9) 
                    setStyle("-fx-background-color:#C8E6C9;-fx-text-fill:#2E7D32;");
                else if (s >= 0.7) 
                    setStyle("-fx-background-color:#FFF9C4;-fx-text-fill:#F57F17;");
                else 
                    setStyle("-fx-background-color:#FFCDD2;-fx-text-fill:#C62828;");
            }
        });
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            // アイコンとテキストを組み合わせて表示
            @Override protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { 
                    setText(null); 
                    setGraphic(null);
                    return; 
                }
                
                HBox container = new HBox();
                container.setAlignment(Pos.CENTER_LEFT);
                container.setSpacing(5);
                
                FontIcon icon = new FontIcon();
                Label label = new Label();
                
                switch (status.toLowerCase(Locale.ROOT)) {
                    case "success" -> {
                        icon.setIconLiteral("fas-check-circle");
                        icon.setIconColor(Color.GREEN);
                        label.setText("成功");
                        label.setTextFill(Color.GREEN);
                    }
                    case "warning" -> {
                        icon.setIconLiteral("fas-exclamation-triangle");
                        icon.setIconColor(Color.ORANGE);
                        label.setText("警告");
                        label.setTextFill(Color.ORANGE);
                    }
                    case "error" -> {
                        icon.setIconLiteral("fas-times-circle");
                        icon.setIconColor(Color.RED);
                        label.setText("エラー");
                        label.setTextFill(Color.RED);
                    }
                    default -> {
                        label.setText(status);
                    }
                }
                
                // アイコンが設定されていれば表示、なければテキストのみ
                if (icon.getIconLiteral() != null) {
                    container.getChildren().addAll(icon, label);
                } else {
                    container.getChildren().add(label);
                }
                
                // セルに設定
                setText(null);
                setGraphic(container);
            }
        });

        // 長文列は折り返し表示 & 可変行高
        scriptColumn.setCellFactory(col -> createWrappingCell(col));
        recognizedColumn.setCellFactory(col -> createWrappingCell(col));

        resultTable.setFixedCellSize(Region.USE_COMPUTED_SIZE);
        resultTable.setItems(results);
    }

    /**
     * 折り返しセルを作成するユーティリティ
     * @param col 対象のTableColumn
     * @return 折り返し表示を行うTableCell
     */
    private <S> TableCell<S, String> createWrappingCell(TableColumn<S, String> col) {
        // 折り返しセル
        return new TableCell<>() {
            private final Text text = new Text();
            {
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(16));
            }
            
            // アイテム更新時にテキストを設定
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setPrefHeight(Region.USE_COMPUTED_SIZE);
                }
            }
        };
    }

    /**
     * CSVプレビュー用テーブルのセットアップ
     * @param data CSVデータのリスト
     */
    private void setupCsvPreviewTable(List<String[]> data) {
        csvPreviewTable.getColumns().clear();
        if (data.isEmpty()) 
            return;
        
        String[] headers = data.get(0);
        for (int i = 0; i < headers.length; i++) {
            final int idx = i;
            
            TableColumn<ObservableList<String>, String> col = new TableColumn<>(headers[i]);
            col.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(
                    param.getValue().size() > idx ? param.getValue().get(idx) : ""));
            col.setCellFactory(c -> new TableCell<>() {
                private final Text text = new Text();
                { text.wrappingWidthProperty().bind(col.widthProperty().subtract(16)); }
                
                // アイテム更新時にテキストを設定
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setGraphic(null); setText(null); 
                    } else { 
                        text.setText(item); 
                        setGraphic(text);
                        setContentDisplay(ContentDisplay.GRAPHIC_ONLY); 
                        setPrefHeight(Region.USE_COMPUTED_SIZE); 
                    }
                }
            });
            
            csvPreviewTable.getColumns().add(col);
        }
        
        ObservableList<ObservableList<String>> tableData = FXCollections.observableArrayList();
        for (int i = 1; i < data.size(); i++) 
            tableData.add(FXCollections.observableArrayList(data.get(i)));
        
        csvPreviewTable.setItems(tableData);
        csvPreviewTable.setFixedCellSize(Region.USE_COMPUTED_SIZE);
        
        // テーブルの高さを可変にする設定
        csvPreviewTable.setPrefHeight(Region.USE_COMPUTED_SIZE);
        csvPreviewTable.setMaxHeight(Double.MAX_VALUE);
        
        // データ量に応じて適切な高さを設定（最大500px、最小150px）
        Platform.runLater(() -> {
            int rowCount = Math.min(tableData.size() + 1, 15); // ヘッダー+データ行(最大15行)
            double computedHeight = Math.max(150, Math.min(500, rowCount * 35 + 50)); // 1行35px+ヘッダー分
            csvPreviewTable.setPrefHeight(computedHeight);
        });
    }

    /**
     * デフォルト設定の読み込み
     */
    private void loadDefaultSettings() {
        pythonPathField.setText(prefs.get("pythonPath", "python"));
    }

    /**
     * Whisperモデル選択の表示/非表示更新
     */
    private void updateWhisperModelVisibility() {
        boolean isWhisper = "whisper".equals(engineComboBox.getValue());
        whisperModelLabel.setVisible(isWhisper);
        whisperModelComboBox.setVisible(isWhisper);
    }

    /**
     * エンジンに応じた言語コードの更新
     */
    private void updateLanguageCodeForEngine() {
        switch (engineComboBox.getValue()) {
            case "google" -> languageCodeField.setText("ja-JP");
            case "whisper" -> languageCodeField.setText("ja");
            case "sphinx" -> languageCodeField.setText("en-US");
        }
    }

    /**
     * テーマの切り替え
     * @param e アクションイベント
     */
    @FXML private void toggleTheme(ActionEvent e) {
        // 現在の保存されているテーマ状態を取得
        boolean currentDark = prefs.getBoolean("darkMode", false);
        
        // テーマを反転
        boolean newDark = !currentDark;
        
        // 新しいテーマを適用
        applyTheme(newDark, true);
        
        // 設定を保存
        prefs.putBoolean("darkMode", newDark);
        
        // ボタンの状態を新しいテーマに合わせて更新
        themeToggle.setSelected(newDark);
        updateThemeButton(newDark);
    }

    /**
     * テーマ切り替えボタンの表示更新
     * @param dark 現在のテーマがダークモードかどうか
     */
    private void updateThemeButton(boolean dark) {
        // 現在のモードに応じて、次に切り替え可能なモードを表示
        String buttonText = dark ? "ライトモード" : "ダークモード";
        String iconLiteral = dark ? "fas-sun" : "fas-moon";
        
        // DEBUG: 現在のテーマ状態とボタン表示内容を確認
        System.out.println("DEBUG: updateThemeButton - dark: " + dark + ", buttonText: " + buttonText);
        
        themeToggle.setText(buttonText);
        if (themeIcon != null) {
            themeIcon.setIconLiteral(iconLiteral);
        }
    }

    /**
     * 保存されているテーマ設定を復元して適用
     */
    private void restoreTheme() {
        boolean dark = prefs.getBoolean("darkMode", false);
        
        // DEBUG: 現在の設定値を確認
        System.out.println("DEBUG: Restored theme - darkMode: " + dark);
        
        // まずボタンの見た目だけは即時反映
        themeToggle.setSelected(dark);
        updateThemeButton(dark);

        // Sceneがまだ無い可能性があるため、確実に適用されるように遅延実行/リスナで適用
        Runnable ensureApply = () -> {
            if (!themeInitialized) {
                applyTheme(dark, false);
                themeInitialized = true;
            }
        };

        // すでにSceneがあるなら即適用
        if (statusLabel != null && statusLabel.getScene() != null) {
            ensureApply.run();
            return;
        }

        // 次のtickでSceneが付く場合に対応
        Platform.runLater(ensureApply);

        // Sceneが付与された瞬間に一度だけ適用
        if (statusLabel != null) {
            statusLabel.sceneProperty().addListener((obs, oldSc, newSc) -> {
                if (newSc != null) {
                    ensureApply.run();
                }
            });
        }
    }

    /**
     * テーマの適用
     * @param dark ダークモードにするかどうか
     */
    private void applyTheme(boolean dark) { 
        applyTheme(dark, true); 
    }

    /**
     * テーマの適用
     * @param dark ダークモードにするかどうか
     * @param animate アニメーションを使うかどうか
     */
    private void applyTheme(boolean dark, boolean animate) {
        Scene scene = statusLabel != null ? statusLabel.getScene() : null;
        if (scene == null)
            return;
        
        var currentRoot = scene.getRoot();
        Runnable toggle = () -> {
            var classes = currentRoot.getStyleClass();
            if (dark) {
                if (!classes.contains("dark-mode"))
                    classes.add("dark-mode"); 
            }
            else {
                classes.remove("dark-mode"); 
            }
        };
        
        // アニメーション付きで切り替え
        if (animate && themeInitialized) {
            WritableImage snap = currentRoot.snapshot(new SnapshotParameters(), null);
            ImageView overlay = new ImageView(snap);
            overlay.setMouseTransparent(true);
            StackPane wrapper = new StackPane();
            scene.setRoot(wrapper);
            wrapper.getChildren().add(currentRoot);
            wrapper.getChildren().add(overlay);
            toggle.run();
            FadeTransition fade = new FadeTransition(Duration.millis(280), overlay);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setInterpolator(Interpolator.EASE_BOTH);
            fade.setOnFinished(ev -> {
                wrapper.getChildren().remove(overlay);
                wrapper.getChildren().remove(currentRoot);
                scene.setRoot(currentRoot);
            });
            fade.play();
        } else {
            toggle.run();
        }
    }

    /**
     * パイソンの実行ファイル選択ダイアログ
     */
    @FXML private void browsePythonPath() {
        FileChooser ch = new FileChooser();
        ch.setTitle("Python実行ファイルを選択");
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("実行ファイル", "*.exe", "*"));
        File f = ch.showOpenDialog(pythonPathField.getScene().getWindow());
        if (f != null) { 
            pythonPathField.setText(f.getAbsolutePath()); 
            prefs.put("pythonPath", f.getAbsolutePath()); 
        }
    }

    /**
     * CSVファイル選択ダイアログ
     */
    @FXML private void browseScriptFile() {
        FileChooser ch = new FileChooser();
        ch.setTitle("台本CSVファイルを選択");
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File f = ch.showOpenDialog(scriptPathField.getScene().getWindow());
        if (f != null) 
            scriptPathField.setText(f.getAbsolutePath());
    }

    /**
     * 音声フォルダ選択ダイアログ
     */
    @FXML private void browseAudioFolder() {
        DirectoryChooser ch = new DirectoryChooser();
        ch.setTitle("音声ファイルフォルダを選択");
        File d = ch.showDialog(audioFolderPathField.getScene().getWindow());
        if (d != null) audioFolderPathField.setText(d.getAbsolutePath());
    }

    /**
     * CSVファイルの読み込み
     * @param filePath CSVファイルのパス
     */
    private void loadCsvFile(String filePath) {
        try {
            csvData.clear();
            csvIds.clear();
            csvIds.add("未選択");
            Path p = Paths.get(filePath);
            if (!Files.exists(p))
                return;

            // まずUTF-8で読み込み、文字化けを検知したらWindows-31Jにフォールバック
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            if (isLikelyMojibake(lines)) {
                try {
                    lines = Files.readAllLines(p, Charset.forName("Windows-31J"));
                } catch (Exception ignore) { 
                    /* 失敗してもUTF-8��まま続行 */
                }
            }
            
            if (lines.isEmpty())
                return;
            
            for (int i = 0; i < lines.size(); i++) {
                String[] row = lines.get(i).split(",", -1);
                csvData.add(row);
                
                if (i > 0 && row.length > 0) 
                    csvIds.add(row[0].trim());
            }
            setupCsvPreviewTable(csvData);
            showCardWithAnimation(csvPreviewCard);
            updateStatus("CSVファイルを読み込みました", false);
        } catch (Exception ex) {
            updateStatus("CSVファイルの読み込みに失敗: " + ex.getMessage(), true);
        }
    }

    /**
     * 文字化けの簡易判定
     * @param lines ファイルの行リスト
     * @return 文字化けの可能性が高い場合にtrue
     */
    private boolean isLikelyMojibake(List<String> lines) {
        // 先頭数行に"�"（置換文字）が一定以上あれば文字化けとみなす簡易判定
        int check = Math.min(10, lines.size());
        int junk = 0, total = 0;
        for (int i = 0; i < check; i++) {
            String s = lines.get(i);
            total += s.length();
            
            for (int j = 0; j < s.length(); j++) if (s.charAt(j) == '\uFFFD')
                junk++;
        }
        
        return total > 0 && junk > Math.max(5, total / 20); // 5%超 or 5文字以上
    }


    /**
     * 音声フォルダの読み込み
     * @param folderPath 音声フォルダのパス
     */
    private void loadAudioFolder(String folderPath) {
        try {
            audioMappings.clear();
            Path dir = Paths.get(folderPath);
            if (!Files.exists(dir))
                return;
            
            List<String> audioFiles = Files.list(dir)
                    .filter(path -> {
                        String n = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.endsWith(".wav") || n.endsWith(".mp3") || n.endsWith(".flac");
                    })
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
            
            for (String fn : audioFiles) 
                audioMappings.add(new AudioMapping(fn, "未選択"));
            
            showCardWithAnimation(audioMappingCard);
            updateStatus("音声フォルダを読み込みました (" + audioFiles.size() + "件)", false);
        } catch (Exception ex) {
            updateStatus("音声フォルダの読み込みに失敗: " + ex.getMessage(), true);
        }
    }

    /**
     * 検証処理の開始
     */
    @FXML private void startValidation() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(); 
            return; 
        }
        
        if (isEmpty(scriptPathField.getText()) || isEmpty(audioFolderPathField.getText())) {
            showAlert("エラー", "CSVファイルと音声フォルダを選択してください。");
            return;
        }
        
        String engine = engineComboBox.getValue();
        if (!preflightCheck(engine))
            return;

        List<ProcessingTask> tasks = prepareTasks();
        if (tasks.isEmpty()) {
            showAlert("警告", "処理するファイルが見つかりません。マッピングを確認してください。");
            return;
        }
        
        currentTask = createValidationTask(tasks);
        showCardWithAnimation(resultsCard);
        Thread th = new Thread(currentTask); th.setDaemon(true); th.start();
    }

    /**
     * 処理開始前の事前チェック
     * @param engine 選択されたエンジン
     * @return チェックを通過した場合にtrue
     */
    private boolean preflightCheck(String engine) {
        boolean ffmpegNeeded = false;
        if ("whisper".equals(engine)) {
            ffmpegNeeded = true;
        } else if ("google".equals(engine) || "sphinx".equals(engine)) {
            for (AudioMapping m : audioMappings) {
                if (!"未選択".equals(m.getCsvId())) {
                    String name = m.getFileName().toLowerCase(Locale.ROOT);
                    
                    if (!(name.endsWith(".wav") || name.endsWith(".aiff") || name.endsWith(".aif") || name.endsWith(".flac"))) {
                        ffmpegNeeded = true; break;
                    }
                }
            }
        }
        
        if (ffmpegNeeded && !isFfmpegAvailable()) {
            showAlert("環境不足", "FFmpeg が見つかりません。mp3等を扱う/Whisperを使うにはFFmpegのインストールとPATH設定が必要です。\n詳しくは PYTHON_USAGE_GUIDE.md を参照してください。");
            return false;
        }
        
        return true;
    }

    /**
     * FFmpegが利用可能かどうかをチェック
     * @return 利用可能な場合にtrue
     */
    private boolean isFfmpegAvailable() {
        try {
            Process p = new ProcessBuilder("ffmpeg", "-version").redirectErrorStream(true).start();
            int exit = p.waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 処理タスクの準備
     * @return 準備された処理タスクのリスト
     */
    private List<ProcessingTask> prepareTasks() {
        List<ProcessingTask> list = new ArrayList<>();
        int scriptCol = -1;
        if (!csvData.isEmpty()) {
            String[] headers = csvData.get(0);
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equals(columnNameField.getText().trim())) { 
                    scriptCol = i; 
                    break;
                }
            }
        }
        
        if (scriptCol == -1) 
            return list;
        
        for (AudioMapping m : audioMappings) {
            if (!"未選択".equals(m.getCsvId())) {
                String script = findScriptTextById(m.getCsvId(), scriptCol);
                if (script != null) {
                    String audioPath = Paths.get(audioFolderPathField.getText(), m.getFileName()).toString();
                    list.add(new ProcessingTask(m.getCsvId(), audioPath, script));
                }
            }
        }
        
        return list;
    }

    /**
     * 台本のセリフをIDから検索
     * @param id 検索するID
     * @param col セリフがある列番号
     * @return 見つかった場合にテキスト、見つからなければnull
     */
    private String findScriptTextById(String id, int col) {
        for (int i = 1; i < csvData.size(); i++) {
            String[] row = csvData.get(i);
            if (row.length > 0 && row[0].trim().equals(id) && row.length > col) 
                return row[col].trim();
        }
        
        return null;
    }

    /**
     * 検証タスクの作成
     * @param tasks 処理タスクのリスト
     * @return 作成されたTaskオブジェクト
     */
    private Task<Void> createValidationTask(List<ProcessingTask> tasks) {
        return new Task<>() {
            // 実際の処理は別スレッドで行う
            @Override protected Void call() {
                Platform.runLater(() -> {
                    results.clear();
                    progressContainer.setVisible(true);
                    validateButton.setText("⏹ 停止");
                    progressBar.setProgress(0);
                    progressLabel.setText("処理を開始しています...");
                });
                
                // Pythonスクリプトの呼び出し
                try {
                    TaskListWrapper wrapper = new TaskListWrapper(tasks);
                    Gson gson = new Gson();
                    String json = gson.toJson(wrapper);
                    File tmp = File.createTempFile("voice_validation_", ".json");
                    Files.writeString(tmp.toPath(), json, StandardCharsets.UTF_8);

                    String pythonPath = pythonPathField.getText();
                    String scriptPath = findScriptPath();
                    String engine = engineComboBox.getValue();
                    String language = languageCodeField.getText();
                    String model = "whisper".equals(engine) ? whisperModelComboBox.getValue() : "none";

                    ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath, engine, language, model, tmp.getAbsolutePath());
                    Map<String, String> env = pb.environment();
                    env.put("PYTHONIOENCODING", "utf-8");
                    env.put("PYTHONUTF8", "1");
                    pb.redirectErrorStream(false);
                    Process pr = pb.start();

                    StringBuilder out = new StringBuilder();
                    StringBuilder err = new StringBuilder();

                    // 標準出力と標準エラーを別スレッドで読み取る
                    Thread outReader = new Thread(() -> {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream(), StandardCharsets.UTF_8))) {
                            String line; while ((line = r.readLine()) != null && !isCancelled()) {
                                out.append(line).append('\n');
                            }
                        } catch (IOException ignored) {}
                    }, "stdout-reader");
                    
                    Thread errReader = new Thread(() -> {
                        try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getErrorStream(), StandardCharsets.UTF_8))) {
                            String line; while ((line = r.readLine()) != null && !isCancelled()) {
                                err.append(line).append('\n');
                                // 進捗のパーセントを抽出
                                if (line.matches(".*(\\d+)%.*")) {
                                    try {
                                        String pct = line.replaceAll(".*?(\\d+)%.*", "$1");
                                        double progress = Double.parseDouble(pct) / 100.0;
                                        Platform.runLater(() -> {
                                            progressBar.setProgress(progress);
                                            progressLabel.setText("処理中... " + pct + "%");
                                        });
                                    } catch (NumberFormatException ignore) {}
                                }
                            }
                        } catch (IOException ignored) {}
                    }, "stderr-reader");

                    outReader.setDaemon(true); 
                    errReader.setDaemon(true);
                    outReader.start(); 
                    errReader.start();

                    int exit = pr.waitFor();
                    outReader.join(2000); errReader.join(2000);
                    tmp.delete();
                    if (isCancelled()) 
                        return null;

                    String outStr = out.toString().trim();
                    String errStr = err.toString().trim();

                    if (!errStr.isEmpty()) {
                        java.nio.file.Path logPath = null;
                        try {
                            logPath = java.nio.file.Files.createTempFile("vvt_python_", ".log");
                            java.nio.file.Files.writeString(logPath, errStr, StandardCharsets.UTF_8);
                        } catch (Exception ignore) {
                            
                        }
                        
                        final java.nio.file.Path finalLogPath = logPath;
                        Platform.runLater(() -> updateStatus(
                                "Python出力: " + shorten(errStr, 200) + (finalLogPath != null ? " (詳細: " + finalLogPath.toString() + ")" : ""),
                                false));
                    }

                    boolean parsedAny = false;

                    // まずは標準出力にJSONがあるか試す
                    if (!outStr.isEmpty()) {
                        try {
                            BatchResult batch = gson.fromJson(outStr, BatchResult.class);
                            Platform.runLater(() -> {
                                if (batch != null && batch.results != null) {
                                    for (PythonResult res : batch.results) {
                                        String status = res.error != null ? "error" : (res.similarity >= 0.9 ? "success" : "warning");
                                        results.add(new ValidationResult(res.id, res.similarity, res.script_text, res.recognized_text, status));
                                    }
                                }
                            });
                            parsedAny = true;
                        } catch (Exception parseEx) {
                            Platform.runLater(() -> updateStatus("結果JSONの解析に失敗: " + parseEx.getMessage(), true));
                        }
                    }

                    // outにJSONが無くてもerrにJSONが含まれる場合があるので試す
                    if (!parsedAny && !errStr.isEmpty()) {
                        String jsonCandidate = extractJson(errStr);
                        if (jsonCandidate != null) {
                            try {
                                BatchResult batch = gson.fromJson(jsonCandidate, BatchResult.class);
                                Platform.runLater(() -> {
                                    if (batch != null && batch.results != null) {
                                        for (PythonResult res : batch.results) {
                                            String status = res.error != null ? "error" : (res.similarity >= 0.9 ? "success" : "warning");
                                            results.add(new ValidationResult(res.id, res.similarity, res.script_text, res.recognized_text, status));
                                        }
                                        if (!batch.results.isEmpty()) {
                                            String reason = batch.results.get(0).error;
                                            if (reason != null) 
                                                showDependencyHint(reason);
                                        }
                                    }
                                });
                                
                                parsedAny = true;
                            } catch (Exception ignore) {
                                // JSONではない
                            }
                        }
                    }

                    if (!parsedAny && exit != 0) {
                        Platform.runLater(() -> updateStatus("Pythonが異常終了しました (exit=" + exit + ")", true));
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> updateStatus("処理中にエラー: " + ex.getMessage(), true));
                }
                
                return null;
            }
            
            @Override protected void succeeded() { finishProgress("検証が完了しました (" + results.size() + "件)", false); }
            @Override protected void cancelled() { finishProgress("処理が中止されました", false); }
            @Override protected void failed() { finishProgress("処理が失敗しました", true); }
        };
    }
    
    /**
     * 標準エラー出力からJSON部分を抽出するユーティリティ
     * @param s 標準エラー出力の文字列
     * @return 抽出されたJSON文字列、見つからなければnull
     */
    private String extractJson(String s) {
        int start = s.lastIndexOf('{');
        int end = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String candidate = s.substring(start, end + 1).trim();
            if (candidate.contains("\"results\"")) 
                return candidate;
        }
        
        return null;
    }
    
    /**
     * 依存関係に関するヒントを表示
     * @param reason エラー理由の文字列
     */
    private void showDependencyHint(String reason) {
        String lower = reason.toLowerCase(Locale.ROOT);
        String hint = null;
        if (lower.contains("pydub") || lower.contains("ffmpeg")) {
            hint = "Google/Sphinxでmp3を扱うにはpydubとffmpegが必要です。requirements.txtをpip installし、ffmpegをPATHに追加してください。";
        } else if (lower.contains("speechrecognition")) {
            hint = "SpeechRecognitionが未インストールです。pip install -r requirements.txt を実行してください。";
        } else if (lower.contains("whisper")) {
            hint = "Whisperが未インストールです。pip install -r requirements.txt を実行してください。初回はモデルをダウンロードします。";
        }
        
        if (hint != null) 
            updateStatus(hint, true);
    }
    
    /**
     * ステータス表示の更新
     * @param message 表示するメッセージ
     * @param isError エラーメッセージかどうか
     */
    private void updateStatus(String message, boolean isError) {
        if (statusLabel == null)
            return;
        
        statusLabel.setText(sanitizeText(message));
        
        if (isError) {
            // 赤色で点滅アニメーション
            FadeTransition ft = new FadeTransition(Duration.millis(200), statusLabel);
            ft.setFromValue(1.0);
            ft.setToValue(0.6);
            ft.setCycleCount(2);
            ft.setAutoReverse(true);
            ft.play();
        }
    }
    
    /**
     * 文字列が空かどうかをチェックするユーティリティ
     * @param s チェックする文字列
     * @return 空またはnullの場合にtrue
     */
    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }

    /**
     * カードの表示アニメーション
     * @param card 表示するカードのVBox
     */
    private void showCardWithAnimation(VBox card) {
        if (card == null) 
            return;
        
        if (!card.isVisible()) {
            card.setOpacity(0);
            card.setVisible(true);
            FadeTransition ft = new FadeTransition(Duration.millis(200), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    /**
     * CSVプレビューテーブルの高さ設定
     */
    private void setupTableHeights() {
        csvPreviewTable.setPrefHeight(Region.USE_COMPUTED_SIZE);
        csvPreviewTable.setMaxHeight(Double.MAX_VALUE);
        csvPreviewTable.setFixedCellSize(Region.USE_COMPUTED_SIZE);
    }

    /**
     * Pythonスクリプトのパスを探索
     * @return 見つかった場合に絶対パス、見つからなければ"RecognizeAndCompare.py"
     */
    private String findScriptPath() {
        // カレントディレクトリ直下
        File f1 = new File("RecognizeAndCompare.py");
        if (f1.exists()) 
            return f1.getAbsolutePath();
        
        // 実行Jar/クラスの親ディレクトリ
        try {
            String base = new File(VoiceoverValidationApp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
            File f2 = new File(base, "RecognizeAndCompare.py");
            if (f2.exists()) 
                return f2.getAbsolutePath();
        } catch (Exception ignored) {
            
        }
        
        // プロジェクトルート推定（resources->../../..）
        try {
            Path p = Paths.get(".").toAbsolutePath().normalize();
            while (p != null) {
                File f3 = p.resolve("RecognizeAndCompare.py").toFile();
                if (f3.exists()) 
                    return f3.getAbsolutePath();
                
                p = p.getParent();
            }
        } catch (Exception ignored) {
            
        }
        
        // 見つからなければ名前のみ返す
        return "RecognizeAndCompare.py";
    }

    /**
     * 進捗を終了し、UIを更新
     * @param msg 終了メッセージ
     * @param isError エラーメッセージかどうか
     * @return 見つかった場合に絶対パス、見つからなければnull
     */
    private void finishProgress(String msg, boolean isError) {
        Platform.runLater(() -> {
            progressContainer.setVisible(false);
            validateButton.setText("検証開始");
            updateStatus(msg, isError);
        });
    }

    /**
     * 指定された文字列を最大長に短縮
     * @param s 短縮する文字列
     * @param max 最大長
     * @return 短縮された文字列（必要に応じて末尾に"…"を追加）
     */
    private String shorten(String s, int max) {
        if (s == null) 
            return "";
        if (s.length() <= max) 
            return s;
        
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    /**
    * アラートダイアログの表示
    * @param title ダイアログのタイトル
    * @param content ダイアログの内容
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // ダークモード時も見やすく
        if (statusLabel != null && statusLabel.getScene() != null) {
            alert.initOwner(statusLabel.getScene().getWindow());
        }
        
        alert.showAndWait();
    }
    
    /**
     * タイトルのサニタイズとアイコンのフォールバック 
     */
    private void sanitizeTitle() {
        if (titleLabel == null) 
            return;
        
        String t = titleLabel.getText();
        titleLabel.setText(sanitizeText(t));
    }

    /**
     * 文字列のサニタイズ
     * @param t サニタイズする文字列
     * @return サニタイズされた文字列
     */
    private String sanitizeText(String t) {
        if (t == null) 
            return "";
        
        // 先頭と末尾の不可視文字やBOMを除去
        String cleaned = t
                .replace("\uFEFF", "") // BOM
                .replace("\u200B", "") // Zero width space
                .replace("\u200C", "")
                .replace("\u200D", "")
                .replace("\u2060", "");
        
        // 制御文字を除去
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t' || !Character.isISOControl(c)) {
                sb.append(c);
            }
        }
        
        return sb.toString().trim();
    }

    /**
     * タイトルアイコンのフォールバック設定
     */
    private void fallbackTitleIcon() {
        
    }

    /**
     * 結果のクリア
     */
    @FXML private void clearResults() {
        results.clear();
        updateStatus("結果をクリアしました", false);
    }

    /**
     * 結果のエクスポート
     */
    @FXML private void exportResults() {
        if (results.isEmpty()) {
            showAlert("情報", "エクスポートする結果がありません。");
            return;
        }
        
        // 既定の保存先: ドキュメント/VoiceValidator/results
        File initialDir = new File(getDocumentsDirectory(), "VoiceValidator/results");
        if (!initialDir.exists()) initialDir.mkdirs();
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File defaultFile = new File(initialDir, "results_" + ts + ".csv");
        
        FileChooser ch = new FileChooser();
        ch.setTitle("結果を保存");
        ch.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        try { ch.setInitialDirectory(initialDir); } catch (Exception ignored) {}
        ch.setInitialFileName(defaultFile.getName());
        File f = ch.showSaveDialog(statusLabel != null ? statusLabel.getScene().getWindow() : null);
        if (f == null) 
            return;
        
        try (BufferedWriter w = Files.newBufferedWriter(f.toPath(), StandardCharsets.UTF_8)) {
            w.write("id,similarity,script,recognized,status\n");
            for (ValidationResult r : results) {
                String line = String.join(",",
                        escapeCsv(r.getId()),
                        String.format(Locale.US, "%.4f", r.getSimilarity()),
                        escapeCsv(r.getScriptText()),
                        escapeCsv(r.getRecognizedText()),
                        escapeCsv(r.getStatus()));
                w.write(line); w.write("\n");
            }
            updateStatus("結果を保存しました: " + f.getName(), false);
        } catch (IOException ex) {
            updateStatus("保存に失敗: " + ex.getMessage(), true);
        }
    }

    // ユーザーのドキュメントフォルダを取得（フォールバック込）
    private File getDocumentsDirectory() {
        try {
            File def = FileSystemView.getFileSystemView().getDefaultDirectory();
            if (def != null && def.exists()) return def;
        } catch (Throwable ignored) { }
        File fb = new File(System.getProperty("user.home"), "Documents");
        if (!fb.exists()) fb.mkdirs();
        return fb;
    }

    /**
     * CSV用に文字列をエスケープ
     * @param s エスケープする文字列
     * @return エスケープされた文字列
     */
    private String escapeCsv(String s) {
        if (s == null) 
            return "";
        
        if (s.contains("\"") || s.contains(",") || s.contains("\n") || s.contains("\r")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        
        return s;
    }
    
    /**
     * 環境チェックの実行
     */
    @FXML private void checkEnvironment() {
        String pythonPath = pythonPathField.getText();
        String testScript = findFileUpward("test_python_environment.py");
        if (testScript == null) {
            showAlert("情報", "test_python_environment.py が見つかりませんでした。\nrequirements.txt のインストール状況とFFmpegの有無を手動で確認してください。");
            return;
        }
        
        // 非同期で実行
        Task<Void> t = new Task<>() {
            @Override protected Void call() {
                updateMessage("環境チェックを実行中...");
                try {
                    ProcessBuilder pb = new ProcessBuilder(pythonPath, testScript);
                    Map<String, String> env = pb.environment();
                    env.put("PYTHONIOENCODING", "utf-8");
                    env.put("PYTHONUTF8", "1");
                    pb.redirectErrorStream(true);
                    Process pr = pb.start();
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(pr.getInputStream(), StandardCharsets.UTF_8))) {
                        String line; while ((line = r.readLine()) != null) sb.append(line).append('\n');
                    }
                    
                    int code = pr.waitFor();
                    String msg = sb.toString();
                    Platform.runLater(() -> {
                        updateStatus(code == 0 ? "環境チェックが完了しました" : "環境チェックで問題が見つかりました", code != 0);
                        Alert a = new Alert(code == 0 ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
                        a.setTitle("環境チェック");
                        a.setHeaderText(null);
                        a.setContentText(shorten(msg, 2000));
                        if (statusLabel != null && statusLabel.getScene() != null) 
                            a.initOwner(statusLabel.getScene().getWindow());
                        
                        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                        a.showAndWait();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> updateStatus("環境チェックに失敗: " + ex.getMessage(), true));
                }
                return null;
            }
        }; 
        
        new Thread(t).start();
    }

    /**
     * 指定された名前のファイルを親ディレクトリ方向に探索
     * @param fileName 探索するファイル名
     * @return 見つかった場合に絶対パス、見つからなければnull
     */
    private String findFileUpward(String fileName) {
        try {
            Path p = Paths.get(".").toAbsolutePath().normalize();
            while (p != null) {
                Path cand = p.resolve(fileName);
                if (Files.exists(cand)) 
                    return cand.toString();
                
                p = p.getParent();
            }
        } catch (Exception ignored) {
            
        }
        
        return null;
    }
}
