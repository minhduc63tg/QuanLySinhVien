package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.FormDialog;
import com.sms.client.util.Ui;
import com.sms.common.dto.ChuongTrinhKhungDTO;
import com.sms.common.dto.KhoaDTO;
import com.sms.common.dto.MonChuongTrinhDTO;
import com.sms.common.dto.MonHocDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Quản lý chương trình khung theo khoa: tổng số tín chỉ yêu cầu (khác nhau giữa các khối ngành) và danh
 * sách môn theo học kỳ gợi ý, bắt buộc/tự chọn. Thay đổi chỉ gửi lên server khi bấm "Lưu chương trình".
 */
public final class ChuongTrinhKhungView extends VBox {

    private final ComboBox<KhoaDTO> khoaBox = Ui.combo(List.of(), KhoaDTO::tenKhoa);
    private final TextField tongField = new TextField();
    private final ObservableList<MonChuongTrinhDTO> items = FXCollections.observableArrayList();
    private final TableView<MonChuongTrinhDTO> table = new TableView<>(items);
    private final Label sumLabel = new Label();
    private final Label dirtyLabel = new Label();
    private final Button saveButton = Ui.button("Lưu chương trình", this::save);
    private KhoaDTO current;
    private boolean dirty;
    private boolean switching;

    public ChuongTrinhKhungView() {
        setSpacing(10);
        khoaBox.setPromptText("Chọn khoa");
        khoaBox.setPrefWidth(260);
        tongField.setPrefColumnCount(6);
        tongField.textProperty().addListener((obs, old, text) -> {
            if (!switching) {
                markDirty();
            }
        });
        dirtyLabel.getStyleClass().add("error-label");

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(Ui.col("Học kỳ", (MonChuongTrinhDTO m) -> String.valueOf(m.hocKyGoiY()), 70));
        table.getColumns().add(Ui.col("Mã môn", MonChuongTrinhDTO::maMon, 110));
        table.getColumns().add(Ui.col("Tên môn", MonChuongTrinhDTO::tenMon, 280));
        table.getColumns().add(Ui.col("TC", m -> String.valueOf(m.soTinChi()), 50));
        table.getColumns().add(Ui.col("Loại", m -> m.batBuoc() ? "Bắt buộc" : "Tự chọn", 90));
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(Ui.title("Chương trình khung"),
                Ui.bar(new Label("Khoa (khối ngành):"), khoaBox, new Label("Tổng tín chỉ yêu cầu:"), tongField,
                        saveButton, dirtyLabel),
                sumLabel,
                Ui.bar(Ui.button("Thêm môn", this::addCourse), Ui.button("Sửa dòng", this::editRow),
                        Ui.button("Xóa dòng", this::removeRow)),
                table);
        setEnabled(false);
        refreshSummary();

        khoaBox.valueProperty().addListener((obs, old, value) -> onFacultyChosen(old, value));
        AsyncRunner.run(() -> Api.danhMuc(s -> s.layTatCaKhoa(Api.token())), khoas -> khoaBox.getItems().setAll(khoas));
    }

    private void setEnabled(boolean enabled) {
        tongField.setDisable(!enabled);
        saveButton.setDisable(!enabled);
        table.setDisable(!enabled);
    }

    private void onFacultyChosen(KhoaDTO previous, KhoaDTO chosen) {
        if (chosen == null || chosen.equals(current)) {
            return;
        }
        if (dirty && !Alerts.confirm("Chương trình khung đang sửa chưa được lưu. Bỏ thay đổi và chuyển khoa?")) {
            // Quay lại khoa cũ mà không kích hoạt tải lại
            switching = true;
            khoaBox.setValue(previous);
            switching = false;
            return;
        }
        current = chosen;
        AsyncRunner.run(() -> Api.chuongTrinhKhung(s -> s.layChuongTrinhKhung(Api.token(), chosen.maKhoa())),
                this::show, khoaBox);
    }

    private void show(ChuongTrinhKhungDTO program) {
        switching = true;
        tongField.setText(String.valueOf(program.tongTinChi()));
        items.setAll(program.monHoc());
        switching = false;
        dirty = false;
        dirtyLabel.setText("");
        setEnabled(true);
        refreshSummary();
    }

    private void markDirty() {
        dirty = true;
        dirtyLabel.setText("Chưa lưu thay đổi");
    }

    private void refreshSummary() {
        int all = items.stream().mapToInt(MonChuongTrinhDTO::soTinChi).sum();
        int mandatory = items.stream().filter(MonChuongTrinhDTO::batBuoc).mapToInt(MonChuongTrinhDTO::soTinChi).sum();
        sumLabel.setText("Đã khai báo " + items.size() + " môn: tổng " + all + " tín chỉ, trong đó bắt buộc "
                + mandatory + " tín chỉ. Tổng yêu cầu không được nhỏ hơn số tín chỉ bắt buộc.");
    }

    private void sortItems() {
        List<MonChuongTrinhDTO> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparingInt(MonChuongTrinhDTO::hocKyGoiY)
                .thenComparing(MonChuongTrinhDTO::batBuoc, Comparator.reverseOrder())
                .thenComparing(MonChuongTrinhDTO::maMon));
        items.setAll(sorted);
    }

    private void addCourse() {
        AsyncRunner.run(() -> Api.monHoc(s -> s.layTatCaMon(Api.token())), all -> {
            Set<String> used = items.stream().map(MonChuongTrinhDTO::maMon).collect(Collectors.toSet());
            List<MonHocDTO> available = all.stream().filter(m -> !used.contains(m.maMon())).toList();
            if (available.isEmpty()) {
                Alerts.info("Mọi môn học đã có trong chương trình khung này.");
                return;
            }
            FormDialog dialog = new FormDialog("Thêm môn vào chương trình khung");
            ComboBox<MonHocDTO> monBox = dialog.combo("Môn học", available, null,
                    m -> m.maMon() + " - " + m.tenMon() + " (" + m.soTinChi() + " TC)");
            TextField hocKy = dialog.text("Học kỳ gợi ý (1-12)", "1");
            CheckBox required = new CheckBox("Bắt buộc");
            required.setSelected(true);
            dialog.node("Loại", required);
            AtomicReference<MonChuongTrinhDTO> result = new AtomicReference<>();
            dialog.show("Thêm", () -> {
                if (monBox.getValue() == null) {
                    throw new IllegalArgumentException("Hãy chọn môn học.");
                }
                MonHocDTO mon = monBox.getValue();
                result.set(new MonChuongTrinhDTO(mon.maMon(), mon.tenMon(), mon.soTinChi(),
                        Ui.parseInt(hocKy, "Học kỳ gợi ý", 1, 12), required.isSelected(), null, null));
                return () -> null; // chỉ sửa danh sách cục bộ, chưa gọi server
            }, () -> {
                items.add(result.get());
                sortItems();
                markDirty();
                refreshSummary();
            });
        }, table);
    }

    private void editRow() {
        MonChuongTrinhDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Hãy chọn một dòng trong bảng trước.");
            return;
        }
        FormDialog dialog = new FormDialog("Sửa " + selected.maMon());
        TextField hocKy = dialog.text("Học kỳ gợi ý (1-12)", String.valueOf(selected.hocKyGoiY()));
        CheckBox required = new CheckBox("Bắt buộc");
        required.setSelected(selected.batBuoc());
        dialog.node("Loại", required);
        AtomicReference<MonChuongTrinhDTO> result = new AtomicReference<>();
        dialog.show("Cập nhật", () -> {
            result.set(new MonChuongTrinhDTO(selected.maMon(), selected.tenMon(), selected.soTinChi(),
                    Ui.parseInt(hocKy, "Học kỳ gợi ý", 1, 12), required.isSelected(), null, null));
            return () -> null;
        }, () -> {
            items.set(items.indexOf(selected), result.get());
            sortItems();
            markDirty();
            refreshSummary();
        });
    }

    private void removeRow() {
        MonChuongTrinhDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Hãy chọn một dòng trong bảng trước.");
            return;
        }
        items.remove(selected);
        markDirty();
        refreshSummary();
    }

    private void save() {
        if (current == null) {
            return;
        }
        int total;
        try {
            total = Ui.parseInt(tongField, "Tổng tín chỉ yêu cầu", 0, 500);
        } catch (IllegalArgumentException invalid) {
            Alerts.error(invalid.getMessage());
            return;
        }
        ChuongTrinhKhungDTO dto = new ChuongTrinhKhungDTO(current.maKhoa(), null, total, new ArrayList<>(items));
        AsyncRunner.run(() -> Api.chuongTrinhKhung(s -> s.luuChuongTrinhKhung(Api.token(), dto)), saved -> {
            show(saved);
            Alerts.info("Đã lưu chương trình khung của khoa " + current.tenKhoa() + ".");
        }, saveButton, table);
    }
}
