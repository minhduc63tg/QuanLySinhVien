package com.sms.client.controller;

import com.sms.client.rmi.RmiClient;
import com.sms.client.session.SessionHolder;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.common.rmi.AuthService;
import com.sms.common.rmi.RmiNames;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;

/** Hộp thoại đổi mật khẩu; giữ mở khi có lỗi để người dùng sửa. */
final class ChangePasswordDialog {

    private ChangePasswordDialog() {
    }

    static void show() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đổi mật khẩu");
        ButtonType ok = new ButtonType("Đổi mật khẩu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        PasswordField oldField = new PasswordField();
        PasswordField newField = new PasswordField();
        PasswordField confirmField = new PasswordField();
        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setVisible(false);
        error.managedProperty().bind(error.visibleProperty());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Mật khẩu hiện tại"), oldField);
        grid.addRow(1, new Label("Mật khẩu mới"), newField);
        grid.addRow(2, new Label("Nhập lại mật khẩu mới"), confirmField);
        grid.add(error, 0, 3, 2, 1);
        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(ok);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            String oldPassword = oldField.getText();
            String newPassword = newField.getText();
            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                hien(error, "Vui lòng nhập đầy đủ mật khẩu.");
            } else if (!newPassword.equals(confirmField.getText())) {
                hien(error, "Mật khẩu nhập lại không khớp.");
            } else {
                error.setVisible(false);
                String token = SessionHolder.token();
                AsyncRunner.run(
                        () -> RmiClient.instance().call(RmiNames.AUTH_SERVICE, AuthService.class, service -> {
                            service.doiMatKhau(token, oldPassword, newPassword);
                            return Boolean.TRUE;
                        }),
                        done -> {
                            dialog.setResult(ok);
                            dialog.close();
                            Alerts.info("Đã đổi mật khẩu thành công.");
                        },
                        failure -> hien(error, Alerts.messageOf(failure)),
                        okButton, oldField, newField, confirmField);
            }
        });
        dialog.showAndWait();
    }

    private static void hien(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
    }
}
