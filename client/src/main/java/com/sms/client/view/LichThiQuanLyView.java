package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.FormDialog;
import com.sms.client.util.ThoiGianTiet;
import com.sms.client.util.Ui;
import com.sms.common.dto.GiangVienDTO;
import com.sms.common.dto.LichThiDTO;
import com.sms.common.dto.LopHocPhanDTO;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/** Quản lý xếp lịch thi và phân công giám thị cho các lớp học phần của một học kỳ. */
public final class LichThiQuanLyView extends CrudView<LichThiQuanLyView.Dong> {

    /** Một lớp học phần cùng lịch thi của nó (thi = null nếu chưa xếp). */
    record Dong(LopHocPhanDTO lop, LichThiDTO thi) {
    }

    private final TextField hocKy = new TextField(Api.config().hocKyMacDinh());

    public LichThiQuanLyView() {
        super("Lịch thi và gác thi");
        hocKy.setPrefColumnCount(12);
        hocKy.setOnAction(event -> reload());
        addNode(new Label("Học kỳ:"));
        addNode(hocKy);
        column("Mã LHP", d -> d.lop().maLhp(), 110);
        column("Môn", d -> d.lop().tenMon(), 190);
        column("Sĩ số", d -> String.valueOf(d.lop().soLuongDaDk()), 55);
        column("Ngày thi", d -> d.thi() == null ? "Chưa xếp lịch" : Ui.thu(d.thi().ngayThi()), 150);
        table.getColumns().add(buoiColumn());
        column("Tiết", d -> d.thi() == null ? ""
                : d.thi().tietBatDau() + "-" + (d.thi().tietBatDau() + d.thi().soTiet() - 1), 55);
        column("Giờ", d -> d.thi() == null ? "" : ThoiGianTiet.khoangGio(d.thi().tietBatDau(), d.thi().soTiet()), 110);
        column("Phòng", d -> d.thi() == null ? "" : d.thi().phong(), 70);
        column("Giám thị", d -> d.thi() == null ? "" : String.join(", ", d.thi().tenGiamThi()), 220);
        searchBy(d -> d.lop().maLhp() + " " + d.lop().tenMon());
        action("Xếp / sửa lịch thi", this::form, true);
        action("Xóa lịch thi", this::remove, true);
        load(() -> {
            String semester = hocKy.getText() == null ? "" : hocKy.getText().trim();
            return () -> {
                List<LopHocPhanDTO> classes = Api.lopHocPhan(s -> s.timTheoHocKy(Api.token(), semester));
                Map<String, LichThiDTO> exams = Api.lichThi(s -> s.layLichThiTheoHocKy(Api.token(), semester))
                        .stream().collect(Collectors.toMap(LichThiDTO::maLhp, t -> t));
                return classes.stream().map(c -> new Dong(c, exams.get(c.maLhp()))).toList();
            };
        });
    }

    private TableColumn<Dong, String> buoiColumn() {
        TableColumn<Dong, String> column = Ui.col("Buổi",
                (Dong d) -> d.thi() == null ? "" : ThoiGianTiet.buoi(d.thi().tietBatDau()).ten(), 80);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                getStyleClass().removeAll("buoi-chip", "cal-sang", "cal-chieu", "cal-toi");
                Dong row = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || row == null || row.thi() == null) {
                    setText(null);
                    return;
                }
                setText(text);
                getStyleClass().addAll("buoi-chip", ThoiGianTiet.buoi(row.thi().tietBatDau()).styleClass());
            }
        });
        return column;
    }

    private void remove(Dong row) {
        if (row.thi() == null) {
            Alerts.info("Lớp học phần này chưa có lịch thi.");
            return;
        }
        if (Alerts.confirm("Xóa lịch thi của lớp " + row.lop().maLhp() + "?")) {
            AsyncRunner.run(() -> Api.lichThi(s -> {
                s.xoaLichThi(Api.token(), row.lop().maLhp());
                return null;
            }), done -> reload(), table);
        }
    }

    private void form(Dong row) {
        AsyncRunner.run(() -> Api.giangVien(s -> s.layTatCaGiangVien(Api.token())), teachers -> open(row, teachers), table);
    }

    private void open(Dong row, List<GiangVienDTO> teachers) {
        LichThiDTO old = row.thi();
        FormDialog dialog = new FormDialog("Lịch thi - " + row.lop().maLhp() + " (" + row.lop().tenMon() + ")");
        DatePicker date = dialog.date("Ngày thi", old == null ? null : old.ngayThi());
        TextField tiet = dialog.text("Tiết bắt đầu (1-12)", old == null ? "1" : String.valueOf(old.tietBatDau()));
        TextField soTiet = dialog.text("Số tiết (1-6)", old == null ? "3" : String.valueOf(old.soTiet()));
        Label hint = new Label("Sáng: tiết 1-5, chiều: tiết 6-10, tối: tiết 11-12.");
        dialog.node("Buổi", hint);
        TextField room = dialog.text("Phòng thi", old == null ? "" : old.phong());

        Map<String, BooleanProperty> checked = new HashMap<>();
        Set<String> current = old == null ? Set.of() : new TreeSet<>(old.maGiamThi());
        for (GiangVienDTO gv : teachers) {
            checked.put(gv.maGv(), new SimpleBooleanProperty(current.contains(gv.maGv())));
        }
        ListView<GiangVienDTO> list = new ListView<>();
        list.getItems().setAll(teachers);
        list.setPrefHeight(150);
        list.setCellFactory(CheckBoxListCell.forListView(gv -> checked.get(gv.maGv()), new StringConverter<>() {
            @Override
            public String toString(GiangVienDTO gv) {
                return gv == null ? "" : gv.maGv() + " - " + gv.hoTen();
            }

            @Override
            public GiangVienDTO fromString(String s) {
                return null;
            }
        }));
        dialog.node("Giám thị", new VBox(4, list, new Label("Đánh dấu giảng viên gác thi (tối đa 5).")));

        dialog.show("Lưu", () -> {
            LocalDate day = date.getValue();
            if (day == null) {
                throw new IllegalArgumentException("Hãy chọn ngày thi.");
            }
            int start = Ui.parseInt(tiet, "Tiết bắt đầu", 1, ThoiGianTiet.SO_TIET_TOI_DA);
            int count = Ui.parseInt(soTiet, "Số tiết", 1, 6);
            if (start + count - 1 > ThoiGianTiet.SO_TIET_TOI_DA) {
                throw new IllegalArgumentException("Buổi thi vượt quá tiết " + ThoiGianTiet.SO_TIET_TOI_DA + ".");
            }
            List<String> invigilators = checked.entrySet().stream().filter(e -> e.getValue().get())
                    .map(Map.Entry::getKey).sorted().toList();
            LichThiDTO dto = new LichThiDTO(row.lop().maLhp(), null, null, null, day, start, count,
                    Ui.required(room, "Phòng thi"), invigilators, null, 0);
            return () -> Api.lichThi(s -> s.luuLichThi(Api.token(), dto));
        }, this::reload);
    }
}
