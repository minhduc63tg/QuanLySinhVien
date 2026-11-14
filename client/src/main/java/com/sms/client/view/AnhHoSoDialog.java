package com.sms.client.view;

import com.sms.client.MainApp;
import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AnhHoSoUtil;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Ui;
import java.io.File;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Quản lý ảnh hồ sơ của một sinh viên hoặc giảng viên (dành cho quản lý): xem
 * ảnh hiện tại, chọn ảnh
 * mới (tự cắt 3:4 và nén), xóa ảnh. Thay đổi chỉ gửi lên server khi bấm "Lưu".
 */
final class AnhHoSoDialog {

    private final String ma;
    private final String hoTen;
    private final AvatarView preview = new AvatarView(210, 280);
    private final Label status = new Label("Đang tải ảnh...");
    private final Button chooseButton = Ui.button("Chọn ảnh...", this::choose);
    private final Button removeButton = Ui.button("Xóa ảnh", this::remove);
    private final TextField pathField = new TextField();
    private final Label dbNameLabel = new Label();
    private final Button copyButton = Ui.button("Sao chép đường dẫn", this::copyPath);
    private final Button openButton = Ui.button("Mở tệp ảnh", this::openFile);
    private final Dialog<ButtonType> dialog = new Dialog<>();
    private byte[] pending; // ảnh mới chọn, chưa lưu
    private boolean hasPhoto; // người dùng hiện có ảnh trên server
    private boolean removeRequested;
    private Node saveButton;

    private AnhHoSoDialog(String ma, String hoTen) {
        this.ma = ma;
        this.hoTen = hoTen;
    }

    static void show(String ma, String hoTen) {
        new AnhHoSoDialog(ma, hoTen).open();
    }

    private void open() {
        dialog.setTitle("Ảnh hồ sơ - " + hoTen + " (" + ma + ")");
        ButtonType save = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, new ButtonType("Đóng", ButtonBar.ButtonData.CANCEL_CLOSE));
        saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.setDisable(true);
        removeButton.setDisable(true);

        Label hint = new Label("Ảnh chân dung tỉ lệ 3:4. Hỗ trợ JPG, PNG, GIF, BMP; ảnh được tự cắt phần giữa, "
                + "thu về 300x400 và nén trước khi lưu.");
        hint.setWrapText(true);
        hint.setMaxWidth(260);
        status.setWrapText(true);
        status.setMaxWidth(260);
        pathField.setEditable(false);
        pathField.setPromptText("Chưa có ảnh");
        pathField.setPrefWidth(280);
        Label pathCaption = new Label("Vị trí tệp ảnh trên máy chủ:");
        pathCaption.setWrapText(true);
        pathCaption.setMaxWidth(280);
        dbNameLabel.setWrapText(true);
        dbNameLabel.setMaxWidth(280);
        copyButton.setDisable(true);
        openButton.setDisable(true);
        VBox side = new VBox(10, new Label(hoTen), hint, new HBox(8, chooseButton, removeButton), status,
                pathCaption, pathField, new HBox(8, copyButton, openButton), dbNameLabel);
        side.setAlignment(Pos.TOP_LEFT);
        HBox content = new HBox(20, preview, side);
        content.setPadding(new Insets(16));
        dialog.getDialogPane().setContent(content);
        preview.setPerson(hoTen, null);

        AsyncRunner.run(() -> Api.anhHoSo(s -> s.layAnh(Api.token(), ma)), photo -> {
            hasPhoto = photo != null;
            preview.setPerson(hoTen, photo);
            removeButton.setDisable(!hasPhoto);
            status.setText(hasPhoto ? "Đang dùng ảnh đã lưu." : "Chưa có ảnh hồ sơ.");
        }, error -> {
            status.setText("Không tải được ảnh hiện tại: " + Alerts.messageOf(error));
        });
        // Đường dẫn tải riêng: tệp ảnh bị mất trên máy chủ thì vẫn thấy đường dẫn để xử
        // lý
        AsyncRunner.run(() -> Api.anhHoSo(s -> s.layDuongDan(Api.token(), ma)), path -> {
            pathField.setText(path == null ? "" : path);

            pathField.setTooltip(path == null ? null : new javafx.scene.control.Tooltip(path)); // xem đủ đường dẫn dài
            copyButton.setDisable(path == null);
            openButton.setDisable(path == null);
        }, error -> {
        });

        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            commit();
        });
        dialog.showAndWait();
    }

    private void copyPath() {
        ClipboardContent content = new ClipboardContent();
        content.putString(pathField.getText());
        Clipboard.getSystemClipboard().setContent(content);
        status.setText("Đã sao chép đường dẫn vào clipboard.");
    }

    /**
     * Mở tệp bằng ứng dụng mặc định nếu máy này truy cập được đường dẫn (cùng máy
     * chủ hoặc thư mục chia sẻ mạng).
     */
    private void openFile() {
        File file = new File(pathField.getText());
        if (!file.isFile() || MainApp.host() == null) {
            Alerts.info("Máy này không truy cập được đường dẫn trên vì tệp nằm trên máy chủ.\n"
                    + "Muốn mở trực tiếp từ máy khác, đặt anh.thumuc trong server.properties là một thư mục "
                    + "chia sẻ mạng (ví dụ //MAYCHU/anh).");
            return;
        }
        MainApp.host().showDocument(file.toURI().toString());
    }

    private void choose() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chọn ảnh hồ sơ");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Tệp ảnh", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"));
        File file = chooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
        if (file == null) {
            return;
        }
        status.setText("Đang xử lý ảnh...");
        AsyncRunner.run(() -> AnhHoSoUtil.chuanHoa(file), data -> {
            pending = data;
            removeRequested = false;
            preview.setPerson(hoTen, data);
            removeButton.setDisable(false);
            saveButton.setDisable(false);
            status.setText("Ảnh mới chưa lưu (" + Math.max(1, data.length / 1024) + " KB). Bấm \"Lưu\" để áp dụng.");
        }, error -> {
            status.setText("");
            Alerts.error(error instanceof java.io.IOException ? error.getMessage() : Alerts.messageOf(error));
        }, chooseButton);
    }

    private void remove() {
        pending = null;
        removeRequested = hasPhoto;
        preview.setPerson(hoTen, null);
        removeButton.setDisable(true);
        saveButton.setDisable(!removeRequested);
        status.setText(removeRequested ? "Ảnh sẽ bị xóa khi bấm \"Lưu\"." : "Chưa có ảnh hồ sơ.");
    }

    private void commit() {
        byte[] data = pending;
        boolean delete = removeRequested;
        AsyncRunner.run(() -> {
            if (data != null) {
                Api.anhHoSo(s -> {
                    s.luuAnh(Api.token(), ma, data);
                    return null;
                });
            } else if (delete) {
                Api.anhHoSo(s -> {
                    s.xoaAnh(Api.token(), ma);
                    return null;
                });
            }
            return Boolean.TRUE;
        }, done -> {
            dialog.setResult(ButtonType.OK);
            dialog.close();
        }, error -> status.setText(Alerts.messageOf(error)), saveButton, chooseButton, removeButton);
    }
}
