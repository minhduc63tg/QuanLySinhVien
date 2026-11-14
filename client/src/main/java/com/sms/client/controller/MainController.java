package com.sms.client.controller;

import com.sms.client.nav.SceneNavigator;
import com.sms.client.rmi.RmiClient;
import com.sms.client.session.SessionHolder;
import com.sms.client.util.AsyncRunner;
import com.sms.client.util.Formatters;
import com.sms.client.view.*;
import com.sms.common.dto.SessionDTO;
import com.sms.common.enums.VaiTro;
import com.sms.common.rmi.AuthService;
import com.sms.common.rmi.RmiNames;
import java.util.List;
import java.util.function.Supplier;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Khung điều hướng: menu bên trái theo vai trò, nội dung ở giữa. */
public class MainController {

    /** Một mục menu; view được tạo mới mỗi lần chọn (luôn tải dữ liệu mới nhất). */
    record MucMenu(String ten, Supplier<Node> view) {
    }

    @FXML private Label tenNguoiDungLabel;
    @FXML private Label vaiTroLabel;
    @FXML private VBox menuBox;
    @FXML private StackPane noiDung;
    @FXML private Button dangXuatButton;

    @FXML
    private void initialize() {
        SessionDTO session = SessionHolder.get();
        tenNguoiDungLabel.setText(session.hoTen() != null ? session.hoTen() : session.tenDangNhap());
        vaiTroLabel.setText(Formatters.vaiTro(session.vaiTro()));
        if (session.vaiTro() != VaiTro.QUAN_LY) {
            hienAnhDaiDien(session);
        }
        List<MucMenu> items = menuCho(session.vaiTro());
        for (MucMenu item : items) {
            Button button = new Button(item.ten());
            button.setOnAction(event -> chon(button, item));
            menuBox.getChildren().add(button);
        }
        if (!items.isEmpty()) {
            chon((Button) menuBox.getChildren().get(0), items.get(0));
        }
    }

    /** Ảnh hồ sơ ở đầu thanh bên của sinh viên và giảng viên; lỗi tải ảnh thì bỏ qua, chỉ hiện chữ cái đầu. */
    private void hienAnhDaiDien(SessionDTO session) {
        String name = session.hoTen() != null ? session.hoTen() : session.tenDangNhap();
        AvatarView avatar = new AvatarView(84, 112);
        avatar.setPerson(name, null);
        VBox sidebar = (VBox) tenNguoiDungLabel.getParent();
        sidebar.getChildren().add(0, avatar);
        AsyncRunner.run(() -> RmiClient.instance().call(RmiNames.ANH_HO_SO_SERVICE,
                        com.sms.common.rmi.AnhHoSoService.class, s -> s.layAnh(session.token(), session.tenDangNhap())),
                photo -> avatar.setPerson(name, photo), error -> { });
    }

    private List<MucMenu> menuCho(VaiTro role) {
        return switch (role) {
            case QUAN_LY -> List.of(
                    new MucMenu("Tổng quan", TongQuanView::new),
                    new MucMenu("Khoa", KhoaView::new),
                    new MucMenu("Lớp", LopView::new),
                    new MucMenu("Sinh viên", SinhVienView::new),
                    new MucMenu("Giảng viên", GiangVienView::new),
                    new MucMenu("Môn học", MonHocView::new),
                    new MucMenu("Chương trình khung", ChuongTrinhKhungView::new),
                    new MucMenu("Lớp học phần", LopHocPhanView::new),
                    new MucMenu("Lịch thi", LichThiQuanLyView::new));
            case SINH_VIEN -> List.of(
                    new MucMenu("Thông tin cá nhân", ThongTinCaNhanView::new),
                    new MucMenu("Tiến độ học tập", TienDoHocTapView::new),
                    new MucMenu("Chương trình khung", ChuongTrinhKhungSvView::new),
                    new MucMenu("Đăng ký học phần", DangKyHocPhanView::new),
                    new MucMenu("Thời khóa biểu", () -> new ThoiKhoaBieuView(false)),
                    new MucMenu("Lịch thi", () -> new LichThiView(false)),
                    new MucMenu("Kết quả học tập", KetQuaHocTapView::new));
            case GIANG_VIEN -> List.of(
                    new MucMenu("Thông tin cá nhân", ThongTinGiangVienView::new),
                    new MucMenu("Lớp phụ trách", LopPhuTrachView::new),
                    new MucMenu("Nhập điểm", NhapDiemView::new),
                    new MucMenu("Lịch dạy", () -> new ThoiKhoaBieuView(true)),
                    new MucMenu("Lịch gác thi", () -> new LichThiView(true)));
        };
    }

    private void chon(Button active, MucMenu item) {
        menuBox.getChildren().forEach(node -> node.getStyleClass().remove("menu-active"));
        active.getStyleClass().add("menu-active");
        noiDung.getChildren().setAll(item.view().get());
    }

    @FXML
    private void onDoiMatKhau() {
        ChangePasswordDialog.show();
    }

    @FXML
    private void onDangXuat() {
        String token = SessionHolder.token();
        // Dù server báo lỗi (hết hạn, mất kết nối) vẫn đăng xuất cục bộ
        Runnable thoat = () -> {
            SessionHolder.clear();
            SceneNavigator.showLogin();
        };
        AsyncRunner.run(
                () -> RmiClient.instance().call(RmiNames.AUTH_SERVICE, AuthService.class, service -> {
                    service.dangXuat(token);
                    return Boolean.TRUE;
                }),
                ok -> thoat.run(),
                error -> thoat.run(),
                dangXuatButton);
    }
}
