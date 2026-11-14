package com.sms.server.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;

/**
 * Kho tệp ảnh hồ sơ trên đĩa của máy chủ. Mỗi người một tệp tên ổn định (SV001.jpg, GV001.png) trong một
 * thư mục duy nhất; luôn dùng đường dẫn tuyệt đối đã chuẩn hóa. Ghi qua tệp tạm rồi đổi tên nguyên tử để
 * người đọc không bao giờ thấy tệp ghi dở.
 */
@Slf4j
public final class AnhHoSoKho {

    private final Path thuMuc;

    public AnhHoSoKho(Path thuMuc) throws IOException {
        this.thuMuc = thuMuc.toAbsolutePath().normalize();
        Files.createDirectories(this.thuMuc);
        if (!Files.isDirectory(this.thuMuc) || !Files.isWritable(this.thuMuc)) {
            throw new IOException("Thư mục ảnh hồ sơ không ghi được: " + this.thuMuc);
        }
        log.info("Thư mục lưu ảnh hồ sơ: {}", this.thuMuc);
    }

    Path thuMuc() {
        return thuMuc;
    }

    /** Đường dẫn tuyệt đối đích của ảnh người dùng (theo mã và kiểu ảnh). */
    Path duongDanDich(String maNguoiDung, String kieu) {
        return thuMuc.resolve(tenTep(maNguoiDung, kieu)).toAbsolutePath().normalize();
    }

    /**
     * Tên tệp an toàn: chỉ giữ tên dạng chữ-số-gạch; mã có ký tự khác (dấu cách, gạch chéo, tiếng Việt...) thì
     * mã hóa hex UTF-8 để không thể thoát khỏi thư mục ảnh hay trùng tên tệp khác.
     */
    static String tenTep(String maNguoiDung, String kieu) {
        String base;
        if (maNguoiDung.matches("[A-Za-z0-9_-]{1,40}")) {
            base = maNguoiDung;
        } else {
            StringBuilder hex = new StringBuilder("u");
            for (byte b : maNguoiDung.getBytes(StandardCharsets.UTF_8)) {
                hex.append("%02x".formatted(b));
            }
            base = hex.toString();
        }
        return base + ("image/png".equals(kieu) ? ".png" : ".jpg");
    }

    /** Ghi dữ liệu vào tệp tạm trong cùng thư mục; gọi {@link #chuyen} để đặt vào vị trí đích. */
    Path ghiTam(byte[] data) throws IOException {
        Path temp = Files.createTempFile(thuMuc, ".tmp-", ".part");
        try {
            Files.write(temp, data);
            return temp;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(temp);
            throw e;
        }
    }

    /** Đưa tệp tạm vào đích, thay tệp cũ cùng tên nếu có. */
    void chuyen(Path temp, Path dich) throws IOException {
        try {
            Files.move(temp, dich, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, dich, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Đọc tệp ảnh; từ chối nếu không phải tệp thường hoặc vượt giới hạn kích thước. */
    byte[] doc(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Không có tệp: " + file);
        }
        if (Files.size(file) > AnhHoSoValidator.TOI_DA_BYTE) {
            throw new IOException("Tệp ảnh vượt quá giới hạn kích thước: " + file);
        }
        return Files.readAllBytes(file);
    }

    /**
     * Giá trị lưu trong cơ sở dữ liệu cho một tệp ảnh: chỉ tên tệp, tính từ thư mục ảnh (ví dụ "SV001.jpg").
     * Lưu đường dẫn tương đối nên đổi ổ đĩa hay chuyển máy chủ chỉ cần chép thư mục và sửa anh.thumuc.
     */
    static String tenLuu(Path tep) {
        return tep.getFileName().toString();
    }

    /**
     * Đổi giá trị lưu trong cơ sở dữ liệu thành đường dẫn tuyệt đối trong thư mục ảnh. Chỉ nhận tên tệp đơn
     * (không dấu phân cách, không "..") và kiểm tra kết quả không thoát khỏi thư mục ảnh, nên dữ liệu bị sửa
     * trong cơ sở dữ liệu cũng không đọc được tệp ngoài thư mục này.
     */
    public Path giaiDuongDan(String tenTep) throws IOException {
        if (tenTep == null || tenTep.isBlank()) {
            throw new IOException("Tên tệp ảnh trống");
        }
        try {
            Path resolved = thuMuc.resolve(tenTep).toAbsolutePath().normalize();
            if (!thuMuc.equals(resolved.getParent()) || !tenTep.equals(resolved.getFileName().toString())) {
                throw new IOException("Tên tệp ảnh không hợp lệ: " + tenTep);
            }
            return resolved;
        } catch (java.nio.file.InvalidPathException e) {
            throw new IOException("Tên tệp ảnh không hợp lệ: " + tenTep, e);
        }
    }

    /** Xóa tệp theo giá trị lưu trong cơ sở dữ liệu; null, rỗng hoặc tên hỏng thì bỏ qua. */
    public void xoaTheoTen(String tenTep) {
        if (tenTep == null || tenTep.isBlank()) {
            return;
        }
        try {
            xoaNeuCo(giaiDuongDan(tenTep));
        } catch (IOException e) {
            log.warn("Bỏ qua tên tệp ảnh không hợp lệ: {}", tenTep);
        }
    }

    /** Xóa tệp nếu có; lỗi chỉ ghi log vì tệp mồ côi vô hại. */
    public void xoaNeuCo(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Không xóa được tệp ảnh {}: {}", file, e.toString());
        }
    }
}
