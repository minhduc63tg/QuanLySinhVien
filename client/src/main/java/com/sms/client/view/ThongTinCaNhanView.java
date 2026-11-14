package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.Ui;
import com.sms.common.dto.SinhVienDTO;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** Thông tin cá nhân của sinh viên; chỉ được sửa email và số điện thoại. */
public final class ThongTinCaNhanView extends VBox {

    private final GridPane grid = new GridPane();
    private final TextField email = new TextField();
    private final TextField sdt = new TextField();
    private final Button save = Ui.button("Lưu liên hệ", this::save);
    private final AvatarView avatar = new AvatarView(170, 227);
    private int row;

    public ThongTinCaNhanView() {
        setSpacing(12);
        grid.setHgap(14);
        grid.setVgap(10);
        grid.setPadding(new Insets(8, 0, 0, 0));
        getChildren().addAll(Ui.title("Thông tin cá nhân"), new javafx.scene.layout.HBox(28, avatar, grid));
        AsyncRunner.run(() -> Api.sinhVien(s -> s.layThongTinCuaToi(Api.token())), this::show);
    }

    private void show(SinhVienDTO sv) {
        avatar.setPerson(sv.hoTen(), null);
        // Ảnh tải riêng: lỗi (chưa có ảnh, mất kết nối) thì giữ chữ cái đầu, không làm hỏng màn hình
        AsyncRunner.run(() -> Api.anhHoSo(s -> s.layAnh(Api.token(), sv.maSv())),
                photo -> avatar.setPerson(sv.hoTen(), photo), error -> { });
        grid.getChildren().clear();
        row = 0;
        add("Mã sinh viên", new Label(sv.maSv()));
        add("Họ tên", new Label(sv.hoTen()));
        add("Ngày sinh", new Label(Formatters.date(sv.ngaySinh())));
        add("Giới tính", new Label(sv.gioiTinh() == null ? "" : Formatters.gioiTinh(sv.gioiTinh())));
        add("Lớp", new Label(sv.tenLop() == null ? "" : sv.tenLop()));
        add("Khoa", new Label(sv.tenKhoa() == null ? "" : sv.tenKhoa()));
        add("Trạng thái học tập", new Label(Formatters.trangThai(sv.trangThai())));
        email.setText(sv.email() == null ? "" : sv.email());
        sdt.setText(sv.soDienThoai() == null ? "" : sv.soDienThoai());
        email.setPrefColumnCount(26);
        add("Email", email);
        add("Số điện thoại", sdt);
        grid.add(save, 1, row);
    }

    private void add(String label, javafx.scene.Node value) {
        Label caption = new Label(label);
        caption.getStyleClass().add("user-name");
        grid.add(caption, 0, row);
        grid.add(value, 1, row);
        row++;
    }

    private void save() {
        String mail = Ui.optional(email);
        String phone = Ui.optional(sdt);
        AsyncRunner.run(() -> Api.sinhVien(s -> {
            s.capNhatLienHe(Api.token(), mail, phone);
            return null;
        }), done -> Alerts.info("Đã cập nhật thông tin liên hệ."), save, email, sdt);
    }
}
