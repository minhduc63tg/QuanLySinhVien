package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.FormDialog;
import com.sms.client.util.Ui;
import com.sms.common.dto.GiangVienDTO;
import com.sms.common.dto.KhoaDTO;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public final class GiangVienView extends CrudView<GiangVienDTO> {

    private static final KhoaDTO KHONG_KHOA = new KhoaDTO(null, "(Không thuộc khoa)");

    public GiangVienView() {
        super("Giảng viên");
        column("Mã GV", GiangVienDTO::maGv, 110);
        column("Họ tên", GiangVienDTO::hoTen, 200);
        column("Email", g -> g.email() == null ? "" : g.email(), 200);
        column("Điện thoại", g -> g.soDienThoai() == null ? "" : g.soDienThoai(), 120);
        column("Khoa", g -> g.tenKhoa() == null ? "" : g.tenKhoa(), 200);
        searchBy(g -> g.maGv() + " " + g.hoTen() + " " + (g.email() == null ? "" : g.email()));
        action("Thêm", g -> form(null), false);
        action("Sửa", this::form, true);
        deleteAction(g -> () -> Api.giangVien(s -> {
            s.xoaGiangVien(Api.token(), g.maGv());
            return null;
        }), g -> "giảng viên " + g.hoTen());
        action("Đặt lại mật khẩu", g -> {
            if (Alerts.confirm("Đặt lại mật khẩu của " + g.hoTen() + " về mã " + g.maGv() + "?")) {
                AsyncRunner.run(() -> Api.auth(s -> {
                    s.resetMatKhau(Api.token(), g.maGv());
                    return null;
                }), done -> Alerts.info("Đã đặt lại mật khẩu về " + g.maGv() + "."), table);
            }
        }, true);
        action("Ảnh hồ sơ", g -> AnhHoSoDialog.show(g.maGv(), g.hoTen()), true);
        load(() -> () -> Api.giangVien(s -> s.layTatCaGiangVien(Api.token())));
    }

    private void form(GiangVienDTO existing) {
        AsyncRunner.run(() -> Api.danhMuc(s -> s.layTatCaKhoa(Api.token())),
                khoas -> open(existing, khoas), table);
    }

    private void open(GiangVienDTO existing, List<KhoaDTO> khoas) {
        FormDialog dialog = new FormDialog(existing == null ? "Thêm giảng viên" : "Sửa giảng viên");
        TextField ma = dialog.text("Mã GV", existing == null ? "" : existing.maGv());
        ma.setDisable(existing != null);
        TextField ten = dialog.text("Họ tên", existing == null ? "" : existing.hoTen());
        TextField email = dialog.text("Email", existing == null ? "" : existing.email());
        TextField sdt = dialog.text("Điện thoại", existing == null ? "" : existing.soDienThoai());
        List<KhoaDTO> options = new ArrayList<>();
        options.add(KHONG_KHOA);
        options.addAll(khoas);
        KhoaDTO selected = existing == null || existing.maKhoa() == null ? KHONG_KHOA : khoas.stream()
                .filter(k -> k.maKhoa().equals(existing.maKhoa())).findFirst().orElse(KHONG_KHOA);
        ComboBox<KhoaDTO> khoa = dialog.combo("Khoa", options, selected, KhoaDTO::tenKhoa);
        dialog.show("Lưu", () -> {
            GiangVienDTO dto = new GiangVienDTO(Ui.required(ma, "Mã GV"), Ui.required(ten, "Họ tên"),
                    Ui.optional(email), Ui.optional(sdt),
                    khoa.getValue() == null ? null : khoa.getValue().maKhoa(), null);
            return () -> Api.giangVien(s -> existing == null
                    ? s.themGiangVien(Api.token(), dto) : s.suaGiangVien(Api.token(), dto));
        }, this::reload);
    }
}
