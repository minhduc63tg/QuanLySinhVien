package com.sms.client.view;

import com.sms.client.rmi.Api;
import com.sms.client.util.Alerts;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.util.FormDialog;
import com.sms.client.util.Ui;
import com.sms.client.util.XuatBangDiem;
import com.sms.common.dto.LopHocDTO;
import com.sms.common.dto.SinhVienDTO;
import com.sms.common.enums.GioiTinh;
import com.sms.common.enums.TrangThaiHocTap;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Sinh viên: tìm kiếm, lọc theo lớp/trạng thái, phân trang phía server, thêm/sửa/xóa, đặt lại mật khẩu. */
public final class SinhVienView extends VBox {

    private static final int PAGE_SIZE = 15;

    private final ObservableList<SinhVienDTO> items = FXCollections.observableArrayList();
    private final TableView<SinhVienDTO> table = new TableView<>(items);
    private final TextField searchField = new TextField();
    private final ComboBox<LopHocDTO> lopFilter = Ui.combo(List.of(), LopHocDTO::tenLop);
    private final ComboBox<TrangThaiHocTap> statusFilter = Ui.combo(List.of(TrangThaiHocTap.values()), Formatters::trangThai);
    private final Label pageLabel = new Label();
    private final Button exportButton = new Button("Xuất bảng điểm PDF");
    private int page;
    private long total;
    private final Button prev = Ui.button("‹ Trước", () -> load(page - 1));
    private final Button next = Ui.button("Sau ›", () -> load(page + 1));

    public SinhVienView() {
        setSpacing(10);
        exportButton.setOnAction(event -> withSelection(sv -> AsyncRunner.run(
                () -> Api.diem(s -> s.layBangDiem(Api.token(), sv.maSv())),
                bangDiem -> XuatBangDiem.xuat(exportButton, sv, bangDiem), exportButton)));
        searchField.setPromptText("Mã hoặc họ tên...");
        lopFilter.setPromptText("Tất cả lớp");
        statusFilter.setPromptText("Tất cả trạng thái");
        searchField.setOnAction(event -> load(0));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(Ui.col("Mã SV", SinhVienDTO::maSv, 90));
        table.getColumns().add(Ui.col("Họ tên", SinhVienDTO::hoTen, 190));
        table.getColumns().add(Ui.col("Ngày sinh", s -> Formatters.date(s.ngaySinh()), 100));
        table.getColumns().add(Ui.col("Giới tính", s -> s.gioiTinh() == null ? "" : Formatters.gioiTinh(s.gioiTinh()), 80));
        table.getColumns().add(Ui.col("Lớp", s -> s.tenLop() == null ? "" : s.tenLop(), 150));
        table.getColumns().add(Ui.col("Khoa", s -> s.tenKhoa() == null ? "" : s.tenKhoa(), 150));
        table.getColumns().add(Ui.col("Email", s -> s.email() == null ? "" : s.email(), 180));
        table.getColumns().add(Ui.col("Trạng thái", s -> Formatters.trangThai(s.trangThai()), 110));
        VBox.setVgrow(table, Priority.ALWAYS);

        getChildren().addAll(
                Ui.title("Sinh viên"),
                Ui.bar(searchField, lopFilter, statusFilter, Ui.button("Tìm", () -> load(0)),
                        Ui.button("Bỏ lọc", this::clearFilters)),
                Ui.bar(Ui.button("Thêm", () -> openForm(null)),
                        Ui.button("Sửa", () -> withSelection(this::openForm)),
                        Ui.button("Xóa", () -> withSelection(this::delete)),
                        Ui.button("Đặt lại mật khẩu", () -> withSelection(this::resetPassword)),
                        Ui.button("Ảnh hồ sơ", () -> withSelection(sv -> AnhHoSoDialog.show(sv.maSv(), sv.hoTen()))),
                        exportButton),
                table,
                Ui.bar(prev, pageLabel, next));

        // Danh sách lớp cho bộ lọc, rồi tải trang đầu
        AsyncRunner.run(() -> Api.danhMuc(s -> s.layTatCaLop(Api.token())), lops -> {
            lopFilter.getItems().setAll(lops);
            load(0);
        });
    }

    private void clearFilters() {
        searchField.clear();
        lopFilter.getSelectionModel().clearSelection();
        statusFilter.getSelectionModel().clearSelection();
        load(0);
    }

    private void load(int target) {
        if (target < 0) {
            return;
        }
        String keyword = Ui.optional(searchField);
        String lop = lopFilter.getValue() == null ? null : lopFilter.getValue().maLop();
        TrangThaiHocTap status = statusFilter.getValue();
        AsyncRunner.run(() -> Api.sinhVien(s -> s.timKiem(Api.token(), keyword, lop, status, target, PAGE_SIZE)),
                result -> {
                    page = target;
                    total = result.tongPhanTu();
                    items.setAll(result.noiDung());
                    long pages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
                    pageLabel.setText("Trang " + (page + 1) + "/" + pages + " (" + total + " sinh viên)");
                    prev.setDisable(page == 0);
                    next.setDisable(page + 1 >= pages);
                    // Trang vượt quá do vừa xóa dòng cuối: lùi một trang
                    if (items.isEmpty() && page > 0) {
                        load(page - 1);
                    }
                }, table);
    }

    private void withSelection(java.util.function.Consumer<SinhVienDTO> action) {
        SinhVienDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alerts.info("Hãy chọn một sinh viên trong bảng trước.");
            return;
        }
        action.accept(selected);
    }

    private void delete(SinhVienDTO sv) {
        if (Alerts.confirm("Xóa sinh viên " + sv.hoTen() + " (" + sv.maSv() + ")?")) {
            AsyncRunner.run(() -> Api.sinhVien(s -> {
                s.xoaSinhVien(Api.token(), sv.maSv());
                return null;
            }), done -> load(page), table);
        }
    }

    private void resetPassword(SinhVienDTO sv) {
        if (Alerts.confirm("Đặt lại mật khẩu của " + sv.hoTen() + " về mã " + sv.maSv() + "?")) {
            AsyncRunner.run(() -> Api.auth(s -> {
                s.resetMatKhau(Api.token(), sv.maSv());
                return null;
            }), done -> Alerts.info("Đã đặt lại mật khẩu về " + sv.maSv() + "."), table);
        }
    }

    private void openForm(SinhVienDTO existing) {
        FormDialog dialog = new FormDialog(existing == null ? "Thêm sinh viên" : "Sửa sinh viên");
        TextField ma = dialog.text("Mã SV", existing == null ? "" : existing.maSv());
        ma.setDisable(existing != null);
        TextField ten = dialog.text("Họ tên", existing == null ? "" : existing.hoTen());
        DatePicker ngaySinh = dialog.date("Ngày sinh", existing == null ? null : existing.ngaySinh());
        ComboBox<GioiTinh> gioiTinh = dialog.combo("Giới tính", List.of(GioiTinh.values()),
                existing == null ? null : existing.gioiTinh(), Formatters::gioiTinh);
        TextField email = dialog.text("Email", existing == null ? "" : existing.email());
        TextField sdt = dialog.text("Điện thoại", existing == null ? "" : existing.soDienThoai());
        ComboBox<TrangThaiHocTap> trangThai = dialog.combo("Trạng thái", List.of(TrangThaiHocTap.values()),
                existing == null ? TrangThaiHocTap.DANG_HOC : existing.trangThai(), Formatters::trangThai);
        LopHocDTO selectedLop = existing == null ? null : lopFilter.getItems().stream()
                .filter(l -> l.maLop().equals(existing.maLop())).findFirst().orElse(null);
        ComboBox<LopHocDTO> lop = dialog.combo("Lớp", List.copyOf(lopFilter.getItems()), selectedLop, LopHocDTO::tenLop);
        dialog.show("Lưu", () -> {
            if (lop.getValue() == null) {
                throw new IllegalArgumentException("Hãy chọn lớp.");
            }
            SinhVienDTO dto = new SinhVienDTO(Ui.required(ma, "Mã SV"), Ui.required(ten, "Họ tên"),
                    ngaySinh.getValue(), gioiTinh.getValue(), Ui.optional(email), Ui.optional(sdt),
                    trangThai.getValue(), lop.getValue().maLop(), null, null, null);
            return () -> Api.sinhVien(s -> existing == null
                    ? s.themSinhVien(Api.token(), dto) : s.suaSinhVien(Api.token(), dto));
        }, () -> load(page));
    }
}
