package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.Ui;
import com.sms.client.util.XuatBangDiem;
import com.sms.common.dto.BangDiemDTO;
import com.sms.common.dto.BangDiemHocKyDTO;
import com.sms.common.dto.DiemDTO;
import com.sms.common.dto.SinhVienDTO;
import com.sms.common.enums.TrangThaiHocTap;
import java.util.Locale;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Kết quả học tập: bảng điểm theo học kỳ, điểm chữ, GPA kỳ và tích lũy, nhãn cảnh báo học vụ. */
public final class KetQuaHocTapView extends VBox {

    private record Data(BangDiemDTO bangDiem, SinhVienDTO sinhVien) {
    }

    private final Label gpaLabel = new Label();
    private final Label statusLabel = new Label();
    private final VBox semesters = new VBox(10);
    private final Button exportButton = new Button("Xuất PDF");
    private Data current;

    public KetQuaHocTapView() {
        setSpacing(10);
        gpaLabel.getStyleClass().add("page-title");
        ScrollPane scroll = new ScrollPane(semesters);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        exportButton.setDisable(true); // chỉ bật khi đã tải xong bảng điểm
        exportButton.setOnAction(event -> XuatBangDiem.xuat(exportButton, current.sinhVien(), current.bangDiem()));
        getChildren().addAll(Ui.title("Kết quả học tập"), gpaLabel, statusLabel, Ui.bar(exportButton), scroll);
        AsyncRunner.run(() -> new Data(Api.diem(s -> s.layBangDiemCuaToi(Api.token())),
                Api.sinhVien(s -> s.layThongTinCuaToi(Api.token()))), this::show);
    }

    private void show(Data data) {
        current = data;
        exportButton.setDisable(false);
        gpaLabel.setText("GPA tích lũy (hệ 4): " + Formatters.gpa(data.bangDiem().gpaTichLuy()));
        TrangThaiHocTap status = data.sinhVien().trangThai();
        statusLabel.setText("Tình trạng: " + Formatters.trangThai(status));
        statusLabel.getStyleClass().remove("error-label");
        if (status == TrangThaiHocTap.CANH_BAO) {
            statusLabel.setText("Cảnh báo học vụ: GPA tích lũy đang dưới ngưỡng cho phép.");
            statusLabel.getStyleClass().add("error-label");
        }
        semesters.getChildren().clear();
        if (data.bangDiem().hocKy().isEmpty()) {
            semesters.getChildren().add(new Label("Chưa có dữ liệu đăng ký học phần."));
            return;
        }
        for (BangDiemHocKyDTO term : data.bangDiem().hocKy()) {
            TableView<DiemDTO> table = new TableView<>();
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.getColumns().add(Ui.col("Mã môn", DiemDTO::maMon, 90));
            table.getColumns().add(Ui.col("Tên môn", DiemDTO::tenMon, 220));
            table.getColumns().add(Ui.col("Tín chỉ", d -> String.valueOf(d.soTinChi()), 60));
            table.getColumns().add(Ui.col("Điểm (10)", d -> Formatters.diem(d.diemSo()), 80));
            table.getColumns().add(Ui.col("Điểm chữ", d -> d.diemChu() == null ? "" : d.diemChu(), 80));
            table.getColumns().add(Ui.col("Điểm (4)", d -> d.diemHe4() == null ? ""
                    : String.format(Locale.ROOT, "%.1f", d.diemHe4()), 80));
            table.getColumns().add(Ui.col("Đánh giá", d -> d.dat() == null ? "Chưa có điểm"
                    : d.dat() ? "Đạt" : "Không đạt", 100));
            table.getItems().setAll(term.bangDiem());
            table.setFixedCellSize(28);
            table.setPrefHeight(28 * (term.bangDiem().size() + 1) + 4);
            TitledPane pane = new TitledPane(term.hocKy() + " - GPA học kỳ: " + Formatters.gpa(term.gpa()), table);
            semesters.getChildren().add(pane);
        }
    }
}
