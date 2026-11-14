package com.sms.client.util;

import com.sms.client.MainApp;
import com.sms.common.dto.BangDiemDTO;
import com.sms.common.dto.SinhVienDTO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.stage.Window;

/** Hộp thoại lưu tệp và xuất bảng điểm ra PDF; việc dựng và ghi tệp chạy ở luồng nền. */
public final class XuatBangDiem {

    private XuatBangDiem() {
    }

    /** Gọi trên FX thread. {@code owner} là nút bấm (bị khóa trong lúc ghi tệp). */
    public static void xuat(Node owner, SinhVienDTO sinhVien, BangDiemDTO bangDiem) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Lưu bảng điểm PDF");
        chooser.setInitialFileName(BangDiemPdf.tenTep(sinhVien));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tệp PDF", "*.pdf"));
        Window window = owner.getScene() == null ? null : owner.getScene().getWindow();
        java.io.File chosen = chooser.showSaveDialog(window);
        if (chosen == null) {
            return;
        }
        Path path = chosen.toPath();
        if (!path.getFileName().toString().toLowerCase().endsWith(".pdf")) {
            path = path.resolveSibling(path.getFileName() + ".pdf");
        }
        Path target = path;
        AsyncRunner.run(() -> {
            // Dựng trong bộ nhớ trước để lỗi giữa chừng không để lại tệp dở dang
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            BangDiemPdf.ghi(sinhVien, bangDiem, LocalDate.now(), buffer);
            Files.write(target, buffer.toByteArray());
            return target;
        }, saved -> {
            if (Alerts.confirm("Đã lưu bảng điểm tại:\n" + saved + "\n\nMở tệp ngay?")
                    && MainApp.host() != null) {
                MainApp.host().showDocument(saved.toUri().toString());
            }
        }, error -> {
            if (error instanceof IOException io) {
                Alerts.error(io.getMessage() == null ? "Không ghi được tệp PDF." : io.getMessage());
            } else {
                Alerts.handle(error);
            }
        }, owner);
    }
}
