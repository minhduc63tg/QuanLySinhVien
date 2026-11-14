package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.FormDialog;
import com.sms.client.util.Ui;
import com.sms.common.dto.KhoaDTO;
import javafx.scene.control.TextField;

public final class KhoaView extends CrudView<KhoaDTO> {

    public KhoaView() {
        super("Khoa");
        column("Mã khoa", KhoaDTO::maKhoa, 140);
        column("Tên khoa", KhoaDTO::tenKhoa, 400);
        searchBy(k -> k.maKhoa() + " " + k.tenKhoa());
        action("Thêm", k -> form(null), false);
        action("Sửa", this::form, true);
        deleteAction(k -> () -> Api.danhMuc(s -> {
            s.xoaKhoa(Api.token(), k.maKhoa());
            return null;
        }), k -> "khoa " + k.tenKhoa());
        load(() -> () -> Api.danhMuc(s -> s.layTatCaKhoa(Api.token())));
    }

    private void form(KhoaDTO existing) {
        FormDialog dialog = new FormDialog(existing == null ? "Thêm khoa" : "Sửa khoa");
        TextField ma = dialog.text("Mã khoa", existing == null ? "" : existing.maKhoa());
        ma.setDisable(existing != null);
        TextField ten = dialog.text("Tên khoa", existing == null ? "" : existing.tenKhoa());
        dialog.show("Lưu", () -> {
            KhoaDTO dto = new KhoaDTO(Ui.required(ma, "Mã khoa"), Ui.required(ten, "Tên khoa"));
            return () -> Api.danhMuc(s -> existing == null
                    ? s.themKhoa(Api.token(), dto) : s.suaKhoa(Api.token(), dto));
        }, this::reload);
    }
}
