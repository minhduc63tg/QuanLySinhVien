package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.FormDialog;
import com.sms.client.util.Ui;
import com.sms.common.dto.MonHocDTO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;

public final class MonHocView extends CrudView<MonHocDTO> {

    public MonHocView() {
        super("Môn học");
        column("Mã môn", MonHocDTO::maMon, 110);
        column("Tên môn", MonHocDTO::tenMon, 260);
        column("Tín chỉ", m -> String.valueOf(m.soTinChi()), 80);
        column("Môn tiên quyết", m -> m.maMonTienQuyet() == null ? ""
                : String.join(", ", new TreeSet<>(m.maMonTienQuyet())), 260);
        searchBy(m -> m.maMon() + " " + m.tenMon());
        action("Thêm", m -> form(null), false);
        action("Sửa", this::form, true);
        deleteAction(m -> () -> Api.monHoc(s -> {
            s.xoaMon(Api.token(), m.maMon());
            return null;
        }), m -> "môn " + m.tenMon());
        load(() -> () -> Api.monHoc(s -> s.layTatCaMon(Api.token())));
    }

    private void form(MonHocDTO existing) {
        AsyncRunner.run(() -> Api.monHoc(s -> s.layTatCaMon(Api.token())),
                all -> open(existing, all), table);
    }

    private void open(MonHocDTO existing, List<MonHocDTO> all) {
        FormDialog dialog = new FormDialog(existing == null ? "Thêm môn học" : "Sửa môn học");
        TextField ma = dialog.text("Mã môn", existing == null ? "" : existing.maMon());
        ma.setDisable(existing != null);
        TextField ten = dialog.text("Tên môn", existing == null ? "" : existing.tenMon());
        TextField tinChi = dialog.text("Số tín chỉ (1-10)", existing == null ? "3" : String.valueOf(existing.soTinChi()));

        // Hộp đánh dấu môn tiên quyết (không gồm chính môn đang sửa)
        Map<String, BooleanProperty> checked = new HashMap<>();
        List<MonHocDTO> candidates = all.stream()
                .filter(m -> existing == null || !m.maMon().equals(existing.maMon())).toList();
        Set<String> current = existing == null || existing.maMonTienQuyet() == null
                ? Set.of() : existing.maMonTienQuyet();
        for (MonHocDTO m : candidates) {
            checked.put(m.maMon(), new SimpleBooleanProperty(current.contains(m.maMon())));
        }
        ListView<MonHocDTO> list = new ListView<>();
        list.getItems().setAll(candidates);
        list.setPrefHeight(180);
        list.setCellFactory(CheckBoxListCell.forListView(m -> checked.get(m.maMon()), new javafx.util.StringConverter<>() {
            @Override
            public String toString(MonHocDTO m) {
                return m == null ? "" : m.maMon() + " - " + m.tenMon();
            }

            @Override
            public MonHocDTO fromString(String s) {
                return null;
            }
        }));
        dialog.node("Môn tiên quyết", new VBox(4, list, new Label("Đánh dấu các môn phải đạt trước.")));

        java.util.concurrent.atomic.AtomicBoolean created = new java.util.concurrent.atomic.AtomicBoolean();
        dialog.show("Lưu", () -> {
            MonHocDTO dto = new MonHocDTO(Ui.required(ma, "Mã môn"), Ui.required(ten, "Tên môn"),
                    Ui.parseInt(tinChi, "Số tín chỉ", 1, 10), Set.of());
            Set<String> prerequisites = new TreeSet<>();
            checked.forEach((code, on) -> {
                if (on.get()) prerequisites.add(code);
            });
            return () -> {
                // Nếu bước tiên quyết lỗi, môn đã được tạo: lần lưu lại không thêm trùng
                if (existing == null && !created.get()) {
                    Api.monHoc(s -> s.themMon(Api.token(), dto));
                    created.set(true);
                } else {
                    Api.monHoc(s -> s.suaMon(Api.token(), dto));
                }
                // Server kiểm tra không tự trỏ và không tạo vòng tiên quyết
                Api.monHoc(s -> {
                    s.capNhatTienQuyet(Api.token(), dto.maMon(), prerequisites);
                    return null;
                });
                return null;
            };
        }, this::reload);
    }
}
