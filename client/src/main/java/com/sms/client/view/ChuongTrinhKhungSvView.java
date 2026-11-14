package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.Ui;
import com.sms.common.dto.MonChuongTrinhDTO;
import com.sms.common.dto.TienDoHocTapDTO;
import com.sms.common.enums.TrangThaiMonKhung;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Chương trình khung của khoa, nhóm theo học kỳ gợi ý, kèm trạng thái từng môn của sinh viên. */
public final class ChuongTrinhKhungSvView extends VBox {

    private final Label summary = new Label();
    private final VBox terms = new VBox(10);

    public ChuongTrinhKhungSvView() {
        setSpacing(10);
        ScrollPane scroll = new ScrollPane(terms);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        getChildren().addAll(Ui.title("Chương trình khung"), summary, scroll);
        AsyncRunner.run(() -> Api.chuongTrinhKhung(s -> s.layTienDoCuaToi(Api.token())), this::show);
    }

    private void show(TienDoHocTapDTO progress) {
        var program = progress.chuongTrinh();
        terms.getChildren().clear();
        if (program.monHoc().isEmpty()) {
            summary.setText("Khoa " + program.tenKhoa() + " chưa có chương trình khung.");
            return;
        }
        summary.setText("Khoa: " + program.tenKhoa() + "  |  Tổng tín chỉ yêu cầu: " + progress.tinChiYeuCau()
                + "  |  Đã tích lũy: " + progress.tinChiTichLuy() + "  |  Đang học: " + progress.tinChiDangHoc());
        Map<Integer, List<MonChuongTrinhDTO>> byTerm = program.monHoc().stream()
                .collect(Collectors.groupingBy(MonChuongTrinhDTO::hocKyGoiY, TreeMap::new, Collectors.toList()));
        byTerm.forEach((term, items) -> {
            int credits = items.stream().mapToInt(MonChuongTrinhDTO::soTinChi).sum();
            int done = items.stream().filter(m -> m.trangThai() == TrangThaiMonKhung.HOAN_THANH)
                    .mapToInt(MonChuongTrinhDTO::soTinChi).sum();
            TableView<MonChuongTrinhDTO> table = table(items);
            TitledPane pane = new TitledPane("Học kỳ " + term + "  —  " + credits + " tín chỉ (đã đạt " + done + ")",
                    table);
            terms.getChildren().add(pane);
        });
    }

    private static TableView<MonChuongTrinhDTO> table(List<MonChuongTrinhDTO> items) {
        TableView<MonChuongTrinhDTO> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(Ui.col("Tên học phần", MonChuongTrinhDTO::tenMon, 260));
        table.getColumns().add(Ui.col("Mã HP", MonChuongTrinhDTO::maMon, 90));
        table.getColumns().add(Ui.col("TC", m -> String.valueOf(m.soTinChi()), 50));
        table.getColumns().add(Ui.col("Loại", m -> m.batBuoc() ? "Bắt buộc" : "Tự chọn", 90));
        table.getColumns().add(Ui.col("Điểm", m -> Formatters.diem(m.diemSo()), 60));
        table.getColumns().add(statusColumn());
        table.getItems().setAll(items);
        table.setFixedCellSize(30);
        table.setPrefHeight(30 * items.size() + 44); // 44 = hàng tiêu đề + viền
        return table;
    }

    private static TableColumn<MonChuongTrinhDTO, String> statusColumn() {
        TableColumn<MonChuongTrinhDTO, String> column = Ui.col("Trạng thái",
                m -> label(m.trangThai()), 120);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                getStyleClass().removeAll("status-done", "status-doing", "status-failed", "status-todo");
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                setText(text);
                getStyleClass().add(switch (getTableRow().getItem().trangThai()) {
                    case HOAN_THANH -> "status-done";
                    case DANG_HOC -> "status-doing";
                    case CHUA_DAT -> "status-failed";
                    case CHUA_HOC -> "status-todo";
                });
            }
        });
        return column;
    }

    private static String label(TrangThaiMonKhung status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case HOAN_THANH -> "✔ Hoàn thành";
            case DANG_HOC -> "● Đang học";
            case CHUA_DAT -> "✖ Chưa đạt";
            case CHUA_HOC -> "○ Chưa học";
        };
    }
}
