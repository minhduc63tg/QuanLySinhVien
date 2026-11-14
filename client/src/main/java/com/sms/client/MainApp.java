package com.sms.client;

import atlantafx.base.theme.PrimerLight;
import com.sms.client.nav.SceneNavigator;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainApp extends Application {

    private static HostServices hostServices;

    @Override
    public void start(Stage stage) {
        hostServices = getHostServices();
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        SceneNavigator.init(stage);
        SceneNavigator.showLogin();
        log.info("Client đã khởi động với theme PrimerLight.");
    }

    /** Mở tệp hoặc liên kết bằng ứng dụng mặc định của hệ điều hành; null nếu chưa khởi động (ví dụ trong test). */
    public static HostServices host() {
        return hostServices;
    }
}
