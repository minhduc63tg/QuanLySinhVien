package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.Ui;
import com.sms.common.dto.DiemDTO;
import com.sms.common.dto.LopHocPhanDTO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableCell;
import javafx.geometry.Pos;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Nhập điểm: bảng chỉnh sửa trực tiếp, lưu hàng loạt các dòng đã thay đổi. */
public final class NhapDiemView extends VBox {

    /** Một dòng điểm có thể sửa; original dùng để biết dòng nào đã đổi. */
    private static final class Row {
        final DiemDTO dto;
        final String original;
        final StringProperty diem;

        Row(DiemDTO dto) {
            this.dto = dto;
            this.original = Formatters.diem(dto.diemSo());
            this.diem = new SimpleStringProperty(original);
        }
    }

    private final TextField hocKy = new TextField(Api.config().hocKyMacDinh());
    private final ComboBox<LopHocPhanDTO> lop = Ui.combo(List.of(), l -> l.maLhp() + " - " + l.tenMon());
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final TableView<Row> table = new TableView<>(rows);
    private final Button save = Ui.button("Lưu tất cả thay đổi", this::save);
    private final Label hint = new Label("Nhập điểm (thang 10, từ 0 đến 10) vào cột Điểm rồi bấm \"Lưu tất cả thay đổi\".");

    public NhapDiemView() {
        setSpacing(10);
        hocKy.setPrefColumnCount(12);
        lop.setPrefWidth(300);
        lop.setPromptText("Chọn lớp học phần");
        lop.valueProperty().addListener((obs, old, value) -> loadStudents(value));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(Ui.col("Mã SV", (Row r) -> r.dto.maSv(), 100));
        table.getColumns().add(Ui.col("Họ tên", r -> r.dto.hoTen(), 240));
        TableColumn<Row, String> scoreColumn = new TableColumn<>("Điểm (10)");
        scoreColumn.setCellValueFactory(cell -> cell.getValue().diem);
        // Ô nhập luôn hiện sẵn và gắn thẳng vào dữ liệu dòng: gõ xong bấm Lưu là có hiệu lực,
        // không cần Enter (TextFieldTableCell sẽ hủy giá trị khi mất focus).
        scoreColumn.setCellFactory(col -> new TableCell<>() {
            private final TextField field = new TextField();
            private StringProperty bound;

            {
                field.setPrefColumnCount(6);
                field.setPromptText("0 - 10");
                setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            public void updateIndex(int index) {
                super.updateIndex(index);
                refresh();
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                refresh();
            }

            private void refresh() {
                if (bound != null) {
                    field.textProperty().unbindBidirectional(bound);
                    bound = null;
                }
                Row row = getIndex() >= 0 && getIndex() < getTableView().getItems().size()
                        ? getTableView().getItems().get(getIndex()) : null;
                if (row == null) {
                    setGraphic(null);
                    return;
                }
                bound = row.diem;
                field.textProperty().bindBidirectional(bound);
                setGraphic(field);
            }
        });
        scoreColumn.setPrefWidth(110);
        scoreColumn.setSortable(false);
        table.getColumns().add(scoreColumn);
        table.getColumns().add(Ui.col("Điểm chữ", r -> r.dto.diemChu() == null ? "" : r.dto.diemChu(), 90));
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(Ui.title("Nhập điểm"),
                Ui.bar(new Label("Học kỳ:"), hocKy, Ui.button("Tải lớp", this::loadClasses), lop),
                hint, table, Ui.bar(save));
        loadClasses();
    }

    private void loadClasses() {
        String semester = hocKy.getText() == null ? "" : hocKy.getText().trim();
        rows.clear();
        AsyncRunner.run(() -> Api.diem(s -> s.layLopDayCuaToi(Api.token(), semester)), list -> {
            lop.getItems().setAll(list);
            lop.getSelectionModel().clearSelection();
            if (list.isEmpty()) {
                hint.setText("Không có lớp học phần nào trong học kỳ này.");
            } else {
                hint.setText("Nhập điểm (thang 10, từ 0 đến 10) vào cột Điểm rồi bấm \"Lưu tất cả thay đổi\".");
            }
        }, hocKy);
    }

    private void loadStudents(LopHocPhanDTO value) {
        rows.clear();
        if (value == null) {
            return;
        }
        AsyncRunner.run(() -> Api.diem(s -> s.layDanhSachSvTrongLop(Api.token(), value.maLhp())),
                list -> list.forEach(d -> rows.add(new Row(d))), table);
    }

    private void save() {
        LopHocPhanDTO selected = lop.getValue();
        if (selected == null) {
            Alerts.info("Hãy chọn lớp học phần.");
            return;
        }
        Map<String, Double> changes = new LinkedHashMap<>();
        for (Row row : rows) {
            String text = row.diem.get() == null ? "" : row.diem.get().trim();
            if (text.equals(row.original)) {
                continue;
            }
            if (text.isEmpty()) {
                Alerts.error("Không thể xóa điểm đã nhập của sinh viên " + row.dto.maSv() + ".");
                return;
            }
            try {
                double value = Double.parseDouble(text.replace(',', '.'));
                if (value < 0 || value > 10 || Double.isNaN(value)) {
                    throw new NumberFormatException();
                }
                changes.put(row.dto.maSv(), value);
            } catch (NumberFormatException e) {
                Alerts.error("Điểm của " + row.dto.maSv() + " (" + row.dto.hoTen() + ") không hợp lệ: \""
                        + text + "\". Điểm phải từ 0 đến 10.");
                return;
            }
        }
        if (changes.isEmpty()) {
            Alerts.info("Không có thay đổi nào để lưu.");
            return;
        }
        AsyncRunner.run(() -> Api.diem(s -> {
            s.nhapDiemHangLoat(Api.token(), selected.maLhp(), changes);
            return null;
        }), done -> {
            Alerts.info("Đã lưu điểm cho " + changes.size() + " sinh viên.");
            loadStudents(selected);
        }, save, table);
    }
}
