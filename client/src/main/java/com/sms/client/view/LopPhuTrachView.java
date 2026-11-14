package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Ui;
import com.sms.common.dto.LopHocPhanDTO;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Giảng viên: các lớp học phần mình phụ trách theo học kỳ. */
public final class LopPhuTrachView extends CrudView<LopHocPhanDTO> {

    private final TextField hocKy = new TextField(Api.config().hocKyMacDinh());

    public LopPhuTrachView() {
        super("Lớp phụ trách");
        hocKy.setPrefColumnCount(12);
        hocKy.setOnAction(event -> reload());
        addNode(new Label("Học kỳ:"));
        addNode(hocKy);
        column("Mã LHP", LopHocPhanDTO::maLhp, 110);
        column("Môn", l -> l.maMon() + " - " + l.tenMon(), 220);
        column("TC", l -> String.valueOf(l.soTinChi()), 45);
        column("Sĩ số", l -> l.soLuongDaDk() + "/" + l.soLuongToiDa(), 70);
        column("Lịch dạy", l -> Ui.lich(l.lichHoc()), 320);
        searchBy(l -> l.maLhp() + " " + l.tenMon());
        load(() -> {
            String semester = hocKy.getText() == null ? "" : hocKy.getText().trim();
            return () -> Api.diem(s -> s.layLopDayCuaToi(Api.token(), semester));
        });
    }
}
