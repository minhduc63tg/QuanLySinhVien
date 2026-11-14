package com.sms.client.nav;

import java.io.IOException;
import java.io.UncheckedIOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Chuyển giữa các màn hình chính (đăng nhập / khung điều hướng) trên một Stage. */
public final class SceneNavigator {

    private static final String CSS = "/css/app.css";
    private static Stage stage;

    private SceneNavigator() {
    }

    public static void init(Stage primary) {
        stage = primary;
    }

    public static void showLogin() {
        show("/fxml/login.fxml", 460, 420, "Đăng nhập - Quản lý sinh viên");
    }

    public static void showMain() {
        show("/fxml/main.fxml", 1100, 700, "Quản lý sinh viên");
    }

    private static void show(String fxml, double width, double height, String title) {
        try {
            Parent root = FXMLLoader.load(SceneNavigator.class.getResource(fxml));
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(SceneNavigator.class.getResource(CSS).toExternalForm());
            stage.setScene(scene);
            stage.setTitle(title);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            throw new UncheckedIOException("Không tải được giao diện " + fxml, e);
        }
    }
}
