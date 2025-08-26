module kotothing.voicevalidator {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires java.prefs;
    requires java.desktop;

    opens koto_thing.voiceover_validator to javafx.fxml, com.google.gson;
    exports koto_thing.voiceover_validator;
}
