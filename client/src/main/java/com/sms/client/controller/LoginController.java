package com.sms.client.controller;

import com.sms.client.nav.SceneNavigator;
import com.sms.client.rmi.RmiClient;
import com.sms.client.session.SessionHolder;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.common.rmi.AuthService;
import com.sms.common.rmi.RmiNames;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField tenDangNhapField;
    @FXML private PasswordField matKhauField;
    @FXML private Label thongBaoLabel;
    @FXML private Button dangNhapButton;
    @FXML private ProgressIndicator tienTrinh;

    @FXML
    private void initialize() {
        // Nhãn lỗi ẩn thì không chiếm chỗ
        thongBaoLabel.managedProperty().bind(thongBaoLabel.visibleProperty());
    }

    @FXML
    private void onDangNhap() {
        String user = tenDangNhapField.getText() == null ? "" : tenDangNhapField.getText().trim();
        String password = matKhauField.getText() == null ? "" : matKhauField.getText();
        if (user.isEmpty() || password.isEmpty()) {
            hienLoi("Vui lòng nhập tên đăng nhập và mật khẩu.");
            return;
        }
        thongBaoLabel.setVisible(false);
        tienTrinh.setVisible(true);
        AsyncRunner.run(
                () -> RmiClient.instance().call(RmiNames.AUTH_SERVICE, AuthService.class,
                        service -> service.dangNhap(user, password)),
                session -> {
                    tienTrinh.setVisible(false);
                    SessionHolder.set(session);
                    SceneNavigator.showMain();
                },
                error -> {
                    tienTrinh.setVisible(false);
                    matKhauField.clear();
                    hienLoi(Alerts.messageOf(error));
                },
                dangNhapButton, tenDangNhapField, matKhauField);
    }

    private void hienLoi(String message) {
        thongBaoLabel.setText(message);
        thongBaoLabel.setVisible(true);
    }
}
