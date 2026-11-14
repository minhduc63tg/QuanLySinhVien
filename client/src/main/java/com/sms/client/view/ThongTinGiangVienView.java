package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Ui;
import com.sms.common.dto.GiangVienDTO;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Thông tin cá nhân của giảng viên (chỉ xem; do quản lý cập nhật), kèm ảnh hồ sơ. */
public final class ThongTinGiangVienView extends VBox {

    private final GridPane grid = new GridPane();
    private final AvatarView avatar = new AvatarView(170, 227);
    private int row;

    public ThongTinGiangVienView() {
        setSpacing(12);
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 0, 0, 0));
        getChildren().addAll(Ui.title("Thông tin cá nhân"), new HBox(28, avatar, grid));
        AsyncRunner.run(() -> Api.giangVien(s -> s.layThongTinCuaToi(Api.token())), this::show);
    }

    private void show(GiangVienDTO gv) {
        grid.getChildren().clear();
        row = 0;
        add("Mã giảng viên", gv.maGv());
        add("Họ tên", gv.hoTen());
        add("Khoa", gv.tenKhoa());
        add("Email", gv.email());
        add("Số điện thoại", gv.soDienThoai());
        avatar.setPerson(gv.hoTen(), null);
        // Ảnh tải riêng: lỗi (chưa có ảnh, mất kết nối) thì giữ chữ cái đầu, không làm hỏng màn hình
        AsyncRunner.run(() -> Api.anhHoSo(s -> s.layAnh(Api.token(), gv.maGv())),
                photo -> avatar.setPerson(gv.hoTen(), photo), error -> { });
    }

    private void add(String label, String value) {
        Label caption = new Label(label);
        caption.getStyleClass().add("user-name");
        Node text = new Label(value == null ? "" : value);
        grid.add(caption, 0, row);
        grid.add(text, 1, row);
        row++;
    }
}
