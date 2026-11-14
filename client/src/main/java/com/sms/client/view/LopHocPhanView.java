package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.FormDialog;
import com.sms.client.util.Ui;
import com.sms.common.dto.DiemDTO;
import com.sms.common.dto.GiangVienDTO;
import com.sms.common.dto.LichHocDTO;
import com.sms.common.dto.LopHocPhanDTO;
import com.sms.common.dto.MonHocDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public final class LopHocPhanView extends CrudView<LopHocPhanDTO> {

    private static final GiangVienDTO CHUA_PHAN_CONG = new GiangVienDTO(null, "(Chưa phân công)", null, null, null, null);

    private record Lookups(List<MonHocDTO> mon, List<GiangVienDTO> giangVien) {
    }

    private final TextField hocKy = new TextField(Api.config().hocKyMacDinh());

    public LopHocPhanView() {
        super("Lớp học phần");
        hocKy.setPromptText("Học kỳ");
        hocKy.setPrefColumnCount(12);
        hocKy.setOnAction(event -> reload());
        addNode(new javafx.scene.control.Label("Học kỳ:"));
        addNode(hocKy);
        column("Mã LHP", LopHocPhanDTO::maLhp, 110);
        column("Môn", l -> l.maMon() + " - " + l.tenMon(), 200);
        column("TC", l -> String.valueOf(l.soTinChi()), 45);
        column("Giảng viên", l -> l.tenGiangVien() == null ? "" : l.tenGiangVien(), 150);
        column("Sĩ số", l -> l.soLuongDaDk() + "/" + l.soLuongToiDa(), 70);
        column("Lịch học", l -> Ui.lich(l.lichHoc()), 280);
        searchBy(l -> l.maLhp() + " " + l.maMon() + " " + l.tenMon());
        action("Thêm", l -> form(null), false);
        action("Sửa", this::form, true);
        deleteAction(l -> () -> Api.lopHocPhan(s -> {
            s.xoaLopHocPhan(Api.token(), l.maLhp());
            return null;
        }), l -> "lớp học phần " + l.maLhp());
        action("Danh sách đăng ký", this::showRegistrations, true);
        load(() -> {
            String semester = hocKy.getText() == null ? "" : hocKy.getText().trim();
            return () -> Api.lopHocPhan(s -> s.timTheoHocKy(Api.token(), semester));
        });
    }

    private void form(LopHocPhanDTO existing) {
        AsyncRunner.run(() -> new Lookups(Api.monHoc(s -> s.layTatCaMon(Api.token())),
                Api.giangVien(s -> s.layTatCaGiangVien(Api.token()))), data -> open(existing, data), table);
    }

    private void open(LopHocPhanDTO existing, Lookups data) {
        FormDialog dialog = new FormDialog(existing == null ? "Thêm lớp học phần" : "Sửa lớp học phần");
        if (existing == null) {
            // Mã do server sinh: mã môn + số thứ tự 2 chữ số của lớp mở trong học kỳ (ví dụ CTDL01)
            Label note = new Label("Mã lớp học phần tự sinh = mã môn + số thứ tự (ví dụ CTDL01).");
            note.setWrapText(true);
            note.setMaxWidth(440);
            dialog.wide(note);
        } else {
            TextField ma = dialog.text("Mã LHP", existing.maLhp());
            ma.setDisable(true);
        }
        MonHocDTO mon = existing == null ? null : data.mon().stream()
                .filter(m -> m.maMon().equals(existing.maMon())).findFirst().orElse(null);
        ComboBox<MonHocDTO> monBox = dialog.combo("Môn học", data.mon(), mon, m -> m.maMon() + " - " + m.tenMon());
        List<GiangVienDTO> teachers = new ArrayList<>();
        teachers.add(CHUA_PHAN_CONG);
        teachers.addAll(data.giangVien());
        GiangVienDTO gv = existing == null || existing.maGv() == null ? CHUA_PHAN_CONG : teachers.stream()
                .filter(g -> existing.maGv().equals(g.maGv())).findFirst().orElse(CHUA_PHAN_CONG);
        ComboBox<GiangVienDTO> gvBox = dialog.combo("Giảng viên", teachers, gv, GiangVienDTO::hoTen);
        TextField semester = dialog.text("Học kỳ", existing == null ? hocKy.getText() : existing.hocKy());
        TextField capacity = dialog.text("Sĩ số tối đa", existing == null ? "50" : String.valueOf(existing.soLuongToiDa()));
        LichEditor lich = new LichEditor(existing == null ? List.of() : existing.lichHoc());
        dialog.node("Lịch học", lich);
        java.util.concurrent.atomic.AtomicReference<String> created = new java.util.concurrent.atomic.AtomicReference<>();

        dialog.show("Lưu", () -> {
            if (monBox.getValue() == null) {
                throw new IllegalArgumentException("Hãy chọn môn học.");
            }
            List<LichHocDTO> schedule = lich.get();
            LopHocPhanDTO dto = new LopHocPhanDTO(existing == null ? null : existing.maLhp(), monBox.getValue().maMon(), null, 0,
                    gvBox.getValue() == null ? null : gvBox.getValue().maGv(), null,
                    Ui.required(semester, "Học kỳ"), Ui.parseInt(capacity, "Sĩ số tối đa", 1, 1000),
                    existing == null ? 0 : existing.soLuongDaDk(), existing == null ? 0 : existing.version(),
                    schedule);
            return () -> {
                if (existing == null) {
                    created.set(Api.lopHocPhan(s -> s.themLopHocPhan(Api.token(), dto)).maLhp());
                } else {
                    Api.lopHocPhan(s -> s.suaLopHocPhan(Api.token(), dto));
                    if (!sameSchedule(existing.lichHoc(), schedule)) {
                        Api.lopHocPhan(s -> {
                            s.luuLichHoc(Api.token(), dto.maLhp(), schedule);
                            return null;
                        });
                    }
                }
                return null;
            };
        }, () -> {
            if (created.get() != null) {
                Alerts.info("Đã tạo lớp học phần " + created.get() + ".");
            }
            reload();
        });
    }

    private static boolean sameSchedule(List<LichHocDTO> a, List<LichHocDTO> b) {
        return key(a).equals(key(b));
    }

    private static List<String> key(List<LichHocDTO> list) {
        return (list == null ? List.<LichHocDTO>of() : list).stream()
                .map(l -> l.thu() + "|" + l.tietBatDau() + "|" + l.soTiet() + "|" + l.phong())
                .sorted().collect(Collectors.toList());
    }

    private void showRegistrations(LopHocPhanDTO lhp) {
        AsyncRunner.run(() -> Api.diem(s -> s.layDanhSachSvTrongLop(Api.token(), lhp.maLhp())), list -> {
            TableView<DiemDTO> students = new TableView<>();
            students.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            students.getColumns().add(Ui.col("Mã SV", DiemDTO::maSv, 100));
            students.getColumns().add(Ui.col("Họ tên", DiemDTO::hoTen, 220));
            students.getColumns().add(Ui.col("Điểm", d -> Formatters.diem(d.diemSo()), 70));
            students.getColumns().add(Ui.col("Điểm chữ", d -> d.diemChu() == null ? "" : d.diemChu(), 80));
            students.getItems().setAll(list);
            students.setPrefSize(520, 360);
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Danh sách đăng ký - " + lhp.maLhp() + " (" + list.size() + " sinh viên)");
            dialog.getDialogPane().setContent(students);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        }, table);
    }
}
