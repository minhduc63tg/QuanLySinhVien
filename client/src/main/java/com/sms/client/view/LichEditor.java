package com.sms.client.view;

import com.sms.client.util.Alerts;
import com.sms.client.util.ThoiGianTiet;
import com.sms.client.util.Ui;
import com.sms.common.dto.LichHocDTO;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Bảng thêm/xóa dòng lịch học của một lớp học phần. */
final class LichEditor extends VBox {

    private final ObservableList<LichHocDTO> rows = FXCollections.observableArrayList();
    private final TableView<LichHocDTO> table = new TableView<>(rows);
    private final ComboBox<Integer> thu = Ui.combo(List.of(2, 3, 4, 5, 6, 7, 8), Ui::thu);
    private final TextField tiet = new TextField();
    private final TextField soTiet = new TextField();
    private final TextField phong = new TextField();

    LichEditor(List<LichHocDTO> initial) {
        super(6);
        if (initial != null) {
            rows.setAll(initial);
        }
        table.setPrefHeight(150);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(Ui.col("Thứ", (LichHocDTO l) -> Ui.thu(l.thu()), 90));
        table.getColumns().add(Ui.col("Tiết bắt đầu", l -> String.valueOf(l.tietBatDau()), 90));
        table.getColumns().add(Ui.col("Số tiết", l -> String.valueOf(l.soTiet()), 70));
        table.getColumns().add(Ui.col("Phòng", LichHocDTO::phong, 90));
        thu.setPromptText("Thứ");
        tiet.setPromptText("Tiết đầu (1-" + ThoiGianTiet.SO_TIET_TOI_DA + ")");
        soTiet.setPromptText("Số tiết (1-6)");
        phong.setPromptText("Phòng");
        tiet.setPrefColumnCount(8);
        soTiet.setPrefColumnCount(7);
        phong.setPrefColumnCount(7);
        table.setPrefHeight(120);
        table.setMinHeight(100);
        // FlowPane: các ô nhập tự xuống dòng khi cửa sổ hẹp
        FlowPane inputs = new FlowPane(6, 6, thu, tiet, soTiet, phong,
                Ui.button("Thêm dòng", this::add), Ui.button("Xóa dòng", this::remove));
        inputs.setPrefWrapLength(380);
        setMaxWidth(440);
        getChildren().addAll(table, inputs);
    }

    private void add() {
        try {
            if (thu.getValue() == null) {
                throw new IllegalArgumentException("Hãy chọn thứ.");
            }
            int start = Ui.parseInt(tiet, "Tiết bắt đầu", 1, ThoiGianTiet.SO_TIET_TOI_DA);
            int count = Ui.parseInt(soTiet, "Số tiết", 1, 6);
            if (start + count - 1 > ThoiGianTiet.SO_TIET_TOI_DA) {
                throw new IllegalArgumentException("Lịch vượt quá tiết " + ThoiGianTiet.SO_TIET_TOI_DA + ".");
            }
            rows.add(new LichHocDTO(null, thu.getValue(), start, count, Ui.required(phong, "Phòng")));
            tiet.clear();
            soTiet.clear();
        } catch (IllegalArgumentException invalid) {
            Alerts.error(invalid.getMessage());
        }
    }

    private void remove() {
        LichHocDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            rows.remove(selected);
        }
    }

    List<LichHocDTO> get() {
        return new ArrayList<>(rows);
    }
}
