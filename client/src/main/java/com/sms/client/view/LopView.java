package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.FormDialog;
import com.sms.client.util.Ui;
import com.sms.common.dto.KhoaDTO;
import com.sms.common.dto.LopHocDTO;
import java.util.List;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public final class LopView extends CrudView<LopHocDTO> {

    public LopView() {
        super("Lớp");
        column("Mã lớp", LopHocDTO::maLop, 140);
        column("Tên lớp", LopHocDTO::tenLop, 300);
        column("Khoa", LopHocDTO::tenKhoa, 250);
        searchBy(l -> l.maLop() + " " + l.tenLop() + " " + l.tenKhoa());
        action("Thêm", l -> form(null), false);
        action("Sửa", this::form, true);
        deleteAction(l -> () -> Api.danhMuc(s -> {
            s.xoaLop(Api.token(), l.maLop());
            return null;
        }), l -> "lớp " + l.tenLop());
        load(() -> () -> Api.danhMuc(s -> s.layTatCaLop(Api.token())));
    }

    private void form(LopHocDTO existing) {
        AsyncRunner.run(() -> Api.danhMuc(s -> s.layTatCaKhoa(Api.token())),
                khoas -> open(existing, khoas), table);
    }

    private void open(LopHocDTO existing, List<KhoaDTO> khoas) {
        FormDialog dialog = new FormDialog(existing == null ? "Thêm lớp" : "Sửa lớp");
        TextField ma = dialog.text("Mã lớp", existing == null ? "" : existing.maLop());
        ma.setDisable(existing != null);
        TextField ten = dialog.text("Tên lớp", existing == null ? "" : existing.tenLop());
        KhoaDTO selected = existing == null ? null : khoas.stream()
                .filter(k -> k.maKhoa().equals(existing.maKhoa())).findFirst().orElse(null);
        ComboBox<KhoaDTO> khoa = dialog.combo("Khoa", khoas, selected, KhoaDTO::tenKhoa);
        dialog.show("Lưu", () -> {
            if (khoa.getValue() == null) {
                throw new IllegalArgumentException("Hãy chọn khoa.");
            }
            LopHocDTO dto = new LopHocDTO(Ui.required(ma, "Mã lớp"), Ui.required(ten, "Tên lớp"),
                    khoa.getValue().maKhoa(), null);
            return () -> Api.danhMuc(s -> existing == null
                    ? s.themLop(Api.token(), dto) : s.suaLop(Api.token(), dto));
        }, this::reload);
    }
}
