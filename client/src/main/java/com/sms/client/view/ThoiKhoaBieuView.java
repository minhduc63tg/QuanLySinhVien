package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.ThoiGianTiet;
import com.sms.client.util.Ui;
import com.sms.common.dto.LichThiDTO;
import com.sms.common.dto.ThoiKhoaBieuDTO;
import com.sms.common.dto.TietHocDTO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Lịch tuần dùng chung cho sinh viên (thời khóa biểu + lịch thi) và giảng viên (lịch dạy + lịch gác thi).
 * Màu: sáng (vàng), chiều (xanh dương), tối (tím) cho buổi học; đỏ cho buổi thi/gác thi.
 * Lịch học lặp mỗi tuần; lịch thi chỉ hiện đúng tuần có ngày thi.
 */
public final class ThoiKhoaBieuView extends VBox {

    private record Data(ThoiKhoaBieuDTO hocTap, List<LichThiDTO> thi) {
    }

    private final boolean giangVien;
    private final TextField hocKy = new TextField(Api.config().hocKyMacDinh());
    private final DatePicker ngayChon = new DatePicker(LocalDate.now());
    private final Label tuanLabel = new Label();
    private final GridPane grid = new GridPane();
    private final TableView<LichThiDTO> thiTable = new TableView<>();
    private final TitledPane thiPane = new TitledPane();
    private Data data = new Data(new ThoiKhoaBieuDTO("", List.of()), List.of());
    private LocalDate thuHai = LocalDate.now().with(DayOfWeek.MONDAY);

    public ThoiKhoaBieuView(boolean giangVien) {
        this.giangVien = giangVien;
        setSpacing(8);
        hocKy.setPrefColumnCount(12);
        hocKy.setOnAction(event -> reload());
        ngayChon.setPrefWidth(140);
        ngayChon.valueProperty().addListener((obs, old, value) -> {
            if (value != null) {
                showWeek(value);
            }
        });

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        configureExamTable();
        thiPane.setText(giangVien ? "Lịch gác thi trong học kỳ (chọn một dòng để xem tuần đó)"
                : "Lịch thi trong học kỳ (chọn một dòng để xem tuần đó)");
        thiPane.setContent(thiTable);
        thiPane.setExpanded(true);

        getChildren().addAll(Ui.title(giangVien ? "Lịch dạy và lịch gác thi" : "Thời khóa biểu và lịch thi"),
                Ui.bar(new Label("Học kỳ:"), hocKy, Ui.button("Xem", this::reload)),
                Ui.bar(Ui.button("‹ Tuần trước", () -> showWeek(thuHai.minusWeeks(1))), ngayChon,
                        Ui.button("Tuần sau ›", () -> showWeek(thuHai.plusWeeks(1))),
                        Ui.button("Hôm nay", () -> showWeek(LocalDate.now())), tuanLabel),
                legend(), scroll, thiPane);
        showWeek(LocalDate.now());
        if (giangVien) {
            reload();
        } else {
            // Sinh viên: mặc định học kỳ đang mở đăng ký
            AsyncRunner.run(() -> Api.dangKy(s -> s.layHocKyDangKy(Api.token())), semester -> {
                hocKy.setText(semester);
                reload();
            });
        }
    }

    /** Chú thích màu: ba buổi học và lịch thi. */
    private HBox legend() {
        HBox box = Ui.bar(
                swatch("cal-legend-sang", "Sáng (tiết 1-5, " + ThoiGianTiet.khoangGio(1, 5) + ")"),
                swatch("cal-legend-chieu", "Chiều (tiết 6-10, " + ThoiGianTiet.khoangGio(6, 5) + ")"),
                swatch("cal-legend-toi", "Tối (tiết 11-12, " + ThoiGianTiet.khoangGio(11, 2) + ")"),
                swatch("cal-legend-thi", giangVien ? "Gác thi" : "Thi"));
        box.setSpacing(18);
        return box;
    }

    private static HBox swatch(String styleClass, String text) {
        Region color = new Region();
        color.getStyleClass().addAll("cal-swatch", styleClass);
        HBox box = Ui.bar(color, new Label(text));
        box.setSpacing(6);
        return box;
    }

    private void configureExamTable() {
        thiTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        thiTable.getColumns().add(Ui.col("Ngày thi", (LichThiDTO t) -> Ui.thu(t.ngayThi()), 150));
        thiTable.getColumns().add(Ui.col("Giờ", t -> ThoiGianTiet.khoangGio(t.tietBatDau(), t.soTiet()), 110));
        thiTable.getColumns().add(Ui.col("Môn", t -> t.tenMon() + " (" + t.maLhp() + ")", 240));
        thiTable.getColumns().add(Ui.col("Phòng", LichThiDTO::phong, 80));
        thiTable.getColumns().add(Ui.col(giangVien ? "Giám thị cùng" : "Giám thị",
                t -> String.join(", ", t.tenGiamThi()), 220));
        thiTable.setPrefHeight(150);
        thiTable.setMinHeight(110);
        thiTable.getSelectionModel().selectedItemProperty().addListener((obs, old, exam) -> {
            if (exam != null) {
                ngayChon.setValue(exam.ngayThi());
            }
        });
    }

    private void reload() {
        String semester = hocKy.getText() == null ? "" : hocKy.getText().trim();
        AsyncRunner.run(() -> giangVien
                ? new Data(Api.thoiKhoaBieu(s -> s.thoiKhoaBieuGiangVien(Api.token(), semester)),
                        Api.lichThi(s -> s.lichGacThi(Api.token(), semester)))
                : new Data(Api.thoiKhoaBieu(s -> s.thoiKhoaBieuSinhVien(Api.token(), semester)),
                        Api.lichThi(s -> s.lichThiSinhVien(Api.token(), semester))),
                loaded -> {
                    data = loaded;
                    thiTable.getItems().setAll(loaded.thi());
                    draw();
                }, hocKy);
    }

    private void showWeek(LocalDate anyDay) {
        thuHai = anyDay.with(DayOfWeek.MONDAY);
        tuanLabel.setText("Tuần " + Formatters.date(thuHai) + " – " + Formatters.date(thuHai.plusDays(6)));
        if (!anyDay.equals(ngayChon.getValue())) {
            ngayChon.setValue(anyDay);
            return; // listener của ngayChon sẽ gọi lại showWeek
        }
        draw();
    }

    private void draw() {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();
        grid.getColumnConstraints().add(new ColumnConstraints(96));
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints(100, 130, Double.MAX_VALUE);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        grid.getRowConstraints().add(new RowConstraints(40));
        for (int t = 1; t <= ThoiGianTiet.SO_TIET_TOI_DA; t++) {
            grid.getRowConstraints().add(new RowConstraints(46));
        }

        grid.add(header("Tiết", null, false), 0, 0);
        LocalDate today = LocalDate.now();
        for (int d = 2; d <= 8; d++) {
            LocalDate date = thuHai.plusDays(d - 2L);
            grid.add(header(Ui.thu(d), Formatters.date(date), date.equals(today)), d - 1, 0);
        }
        for (int t = 1; t <= ThoiGianTiet.SO_TIET_TOI_DA; t++) {
            Label time = new Label("Tiết " + t + "\n" + ThoiGianTiet.batDau(t) + "-" + ThoiGianTiet.ketThuc(t));
            time.setStyle("-fx-font-size: 11px;");
            StackPane head = new StackPane(time);
            head.getStyleClass().addAll("cal-grid-line", ThoiGianTiet.buoi(t).styleClass());
            grid.add(head, 0, t);
            for (int d = 2; d <= 8; d++) {
                StackPane empty = new StackPane();
                empty.getStyleClass().add("cal-grid-line");
                grid.add(empty, d - 1, t);
            }
        }

        for (TietHocDTO slot : data.hocTap().lichHoc()) {
            String detail = giangVien ? slot.maLhp() : (slot.tenGiangVien() == null ? "" : slot.tenGiangVien());
            grid.add(cell(ThoiGianTiet.buoi(slot.tietBatDau()).styleClass(), slot.tenMon(), slot.phong(), detail),
                    slot.thu() - 1, slot.tietBatDau(), 1, slot.soTiet());
        }
        // Lịch thi đè lên lịch học trong đúng ngày thi
        for (LichThiDTO exam : data.thi()) {
            long offset = java.time.temporal.ChronoUnit.DAYS.between(thuHai, exam.ngayThi());
            if (offset < 0 || offset > 6) {
                continue;
            }
            String title = (giangVien ? "GÁC THI: " : "THI: ") + exam.tenMon();
            String detail = giangVien ? exam.maLhp() + " - " + exam.soThiSinh() + " thí sinh"
                    : "Giám thị: " + String.join(", ", exam.tenGiamThi());
            grid.add(cell("cal-thi", title, exam.phong(), detail),
                    (int) offset + 1, exam.tietBatDau(), 1, exam.soTiet());
        }
    }

    private static VBox header(String title, String date, boolean today) {
        Label name = new Label(title);
        name.getStyleClass().add("user-name");
        VBox box = date == null ? new VBox(name) : new VBox(name, new Label(date));
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("cal-grid-line");
        if (today) {
            box.getStyleClass().add("cal-today");
        }
        return box;
    }

    private static VBox cell(String colorClass, String title, String room, String detail) {
        Label name = new Label(title);
        name.getStyleClass().add("cal-cell-title");
        name.setWrapText(true);
        Label place = new Label("Phòng " + room);
        VBox box = detail == null || detail.isBlank() ? new VBox(2, name, place) : new VBox(2, name, place, wrap(detail));
        box.getStyleClass().addAll("cal-cell", colorClass);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private static Label wrap(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }
}
