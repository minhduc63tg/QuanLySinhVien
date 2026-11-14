package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.Ui;
import com.sms.common.dto.DangKyDTO;
import com.sms.common.dto.LopCoTheDangKyDTO;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Đăng ký học phần: bảng lớp có thể đăng ký (kèm lý do nếu không được) và bảng lớp đã đăng ký. */
public final class DangKyHocPhanView extends VBox {

    private record Data(String hocKy, List<LopCoTheDangKyDTO> lop, List<DangKyDTO> daDangKy) {
    }

    private final Label hocKyLabel = new Label();
    private final Label tinChiLabel = new Label();
    private final TableView<LopCoTheDangKyDTO> available = new TableView<>();
    private final TableView<DangKyDTO> mine = new TableView<>();

    public DangKyHocPhanView() {
        setSpacing(10);
        available.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        available.getColumns().add(Ui.col("Mã LHP", (LopCoTheDangKyDTO c) -> c.lopHocPhan().maLhp(), 100));
        available.getColumns().add(Ui.col("Môn", c -> c.lopHocPhan().tenMon(), 180));
        available.getColumns().add(Ui.col("TC", c -> String.valueOf(c.lopHocPhan().soTinChi()), 40));
        available.getColumns().add(Ui.col("Giảng viên", c -> nz(c.lopHocPhan().tenGiangVien()), 130));
        available.getColumns().add(Ui.col("Sĩ số", c -> c.lopHocPhan().soLuongDaDk() + "/" + c.lopHocPhan().soLuongToiDa(), 60));
        available.getColumns().add(Ui.col("Lịch học", c -> Ui.lich(c.lopHocPhan().lichHoc()), 240));
        available.getColumns().add(Ui.col("Trạng thái", c -> c.coTheDangKy() ? "Có thể đăng ký" : nz(c.thongBao()), 240));

        mine.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        mine.getColumns().add(Ui.col("Mã LHP", (DangKyDTO d) -> d.lopHocPhan().maLhp(), 100));
        mine.getColumns().add(Ui.col("Môn", d -> d.lopHocPhan().tenMon(), 180));
        mine.getColumns().add(Ui.col("TC", d -> String.valueOf(d.lopHocPhan().soTinChi()), 40));
        mine.getColumns().add(Ui.col("Lịch học", d -> Ui.lich(d.lopHocPhan().lichHoc()), 260));
        mine.getColumns().add(Ui.col("Điểm", d -> Formatters.diem(d.diemSo()), 60));
        VBox.setVgrow(available, Priority.ALWAYS);
        VBox.setVgrow(mine, Priority.ALWAYS);

        getChildren().addAll(Ui.title("Đăng ký học phần"),
                Ui.bar(hocKyLabel, tinChiLabel, Ui.button("Tải lại", this::reload)),
                new Label("Lớp có thể đăng ký"), available,
                Ui.bar(Ui.button("Đăng ký", this::register)),
                new Label("Lớp đã đăng ký trong học kỳ"), mine,
                Ui.bar(Ui.button("Hủy đăng ký", this::cancel)));
        reload();
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private void reload() {
        AsyncRunner.run(() -> {
            String semester = Api.dangKy(s -> s.layHocKyDangKy(Api.token()));
            List<LopCoTheDangKyDTO> lop = Api.dangKy(s -> s.layLopCoTheDangKy(Api.token()));
            List<DangKyDTO> all = Api.dangKy(s -> s.layDangKyCuaToi(Api.token()));
            return new Data(semester, lop, all.stream()
                    .filter(d -> semester.equals(d.lopHocPhan().hocKy())).toList());
        }, data -> {
            hocKyLabel.setText("Học kỳ đang mở đăng ký: " + data.hocKy());
            int credits = data.daDangKy().stream().mapToInt(d -> d.lopHocPhan().soTinChi()).sum();
            tinChiLabel.setText("| Tổng tín chỉ đã đăng ký: " + credits);
            available.getItems().setAll(data.lop());
            mine.getItems().setAll(data.daDangKy());
        }, available, mine);
    }

    private void register() {
        LopCoTheDangKyDTO selected = available.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Hãy chọn một lớp trong bảng trên.");
            return;
        }
        if (!selected.coTheDangKy()) {
            Alerts.error(nz(selected.thongBao()).isEmpty() ? "Không thể đăng ký lớp này." : selected.thongBao());
            return;
        }
        String code = selected.lopHocPhan().maLhp();
        AsyncRunner.run(() -> Api.dangKy(s -> {
            s.dangKy(Api.token(), code);
            return null;
        }), done -> {
            Alerts.info("Đăng ký thành công lớp " + code + ".");
            reload();
        }, error -> {
            Alerts.handle(error);
            reload(); // sĩ số có thể đã đổi
        }, available);
    }

    private void cancel() {
        DangKyDTO selected = mine.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Hãy chọn một lớp đã đăng ký.");
            return;
        }
        if (selected.diemSo() != null) {
            Alerts.error("Không thể hủy lớp đã có điểm.");
            return;
        }
        String code = selected.lopHocPhan().maLhp();
        if (Alerts.confirm("Hủy đăng ký lớp " + code + "?")) {
            AsyncRunner.run(() -> Api.dangKy(s -> {
                s.huyDangKy(Api.token(), code);
                return null;
            }), done -> reload(), mine);
        }
    }
}
