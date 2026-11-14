package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.ThoiGianTiet;
import com.sms.client.util.Ui;
import com.sms.common.dto.LichThiDTO;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

/** Danh sách lịch thi của sinh viên hoặc lịch gác thi của giảng viên theo học kỳ. */
public final class LichThiView extends CrudView<LichThiDTO> {

    private final TextField hocKy = new TextField(Api.config().hocKyMacDinh());

    public LichThiView(boolean giangVien) {
        super(giangVien ? "Lịch gác thi" : "Lịch thi");
        hocKy.setPrefColumnCount(12);
        hocKy.setOnAction(event -> reload());
        addNode(new Label("Học kỳ:"));
        addNode(hocKy);
        column("Ngày thi", t -> Ui.thu(t.ngayThi()), 150);
        table.getColumns().add(buoiColumn());
        column("Tiết", t -> t.tietBatDau() + "-" + (t.tietBatDau() + t.soTiet() - 1), 60);
        column("Giờ", t -> ThoiGianTiet.khoangGio(t.tietBatDau(), t.soTiet()), 110);
        column("Môn", t -> t.tenMon() + " (" + t.maLhp() + ")", 230);
        column("Phòng", LichThiDTO::phong, 80);
        if (giangVien) {
            column("Thí sinh", t -> String.valueOf(t.soThiSinh()), 80);
        }
        column(giangVien ? "Giám thị cùng" : "Giám thị", t -> String.join(", ", t.tenGiamThi()), 220);
        searchBy(t -> t.maLhp() + " " + t.tenMon());
        load(() -> {
            String semester = hocKy.getText() == null ? "" : hocKy.getText().trim();
            return () -> giangVien ? Api.lichThi(s -> s.lichGacThi(Api.token(), semester))
                    : Api.lichThi(s -> s.lichThiSinhVien(Api.token(), semester));
        });
        if (!giangVien) {
            // Sinh viên: mặc định học kỳ đang mở đăng ký
            AsyncRunner.run(() -> Api.dangKy(s -> s.layHocKyDangKy(Api.token())), semester -> {
                hocKy.setText(semester);
                reload();
            });
        }
    }

    /** Cột buổi (sáng/chiều/tối) tô màu giống lịch tuần. */
    private TableColumn<LichThiDTO, String> buoiColumn() {
        TableColumn<LichThiDTO, String> column = Ui.col("Buổi",
                (LichThiDTO t) -> ThoiGianTiet.buoi(t.tietBatDau()).ten(), 80);
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                getStyleClass().removeAll("buoi-chip", "cal-sang", "cal-chieu", "cal-toi");
                if (empty || text == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                setText(text);
                getStyleClass().addAll("buoi-chip",
                        ThoiGianTiet.buoi(getTableRow().getItem().tietBatDau()).styleClass());
            }
        });
        return column;
    }

}
