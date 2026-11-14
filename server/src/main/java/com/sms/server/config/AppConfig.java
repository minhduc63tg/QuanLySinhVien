package com.sms.server.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public final class AppConfig {

    private final Properties values;
    private final Path baseDir;

    private AppConfig(Properties values, Path baseDir) {
        this.baseDir = baseDir;
        this.values = values;
        String url = required("db.url");
        if (!url.startsWith("jdbc:mariadb:")) {
            throw new IllegalArgumentException("db.url phải là URL JDBC MariaDB.");
        }
        required("db.user");
        if (values.getProperty("db.password") == null
                || values.getProperty("db.password").contains("CHANGE_ME")) {
            throw new IllegalArgumentException("Hãy điền db.password trong server.properties.");
        }
        registryPort();
        exportPort();
        hostname();
        hocKyDangKy();
        tinChiToiDa();
        diemQua();
        canhBaoGpa();
        soLanThuLai();
        anhThuMuc();
    }

    public static AppConfig load() {
        String override = System.getProperty("sms.config");
        Path path = override == null ? Path.of("server.properties") : Path.of(override);
        if (override == null && !Files.isRegularFile(path)) {
            path = Path.of("..", "server.properties");
        }
        return load(path);
    }

    public static AppConfig load(Path path) {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Không đọc được cấu hình. Sao chép server.properties.example thành server.properties.", e);
        }
        return new AppConfig(properties, path.toAbsolutePath().getParent());
    }

    private String required(String key) {
        String value = values.getProperty(key);
        if (value == null || value.isBlank() || value.contains("CHANGE_ME")) {
            throw new IllegalArgumentException("Hãy điền " + key + " trong server.properties.");
        }
        return value.trim();
    }

    private int integer(String key, int min, int max) {
        try {
            int value = Integer.parseInt(required(key));
            if (value >= min && value <= max) {
                return value;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " phải là số nguyên.", e);
        }
        throw new IllegalArgumentException(key + " phải nằm trong [" + min + ", " + max + "].");
    }

    private double decimal(String key, double max) {
        try {
            double value = Double.parseDouble(required(key));
            if (Double.isFinite(value) && value >= 0 && value <= max) {
                return value;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " phải là số.", e);
        }
        throw new IllegalArgumentException(key + " phải nằm trong [0, " + max + "].");
    }

    public Map<String, Object> jdbcProperties() {
        return Map.of(
                "jakarta.persistence.jdbc.url", required("db.url"),
                "jakarta.persistence.jdbc.user", required("db.user"),
                "jakarta.persistence.jdbc.password", values.getProperty("db.password"),
                "jakarta.persistence.jdbc.driver", "org.mariadb.jdbc.Driver");
    }

    public int registryPort() { return integer("rmi.registry.port", 1, 65535); }
    public int exportPort() { return integer("rmi.export.port", 1, 65535); }
    public String hostname() { return required("rmi.hostname"); }
    public String hocKyDangKy() { return required("dangky.hocky"); }
    public int tinChiToiDa() { return integer("dangky.tinchi.toida", 1, 100); }
    public double diemQua() { return decimal("diem.qua", 10); }
    public double canhBaoGpa() { return decimal("canhbao.gpa", 4); }
    public int soLanThuLai() { return integer("dangky.retry", 1, 100); }

    /**
     * Thư mục lưu tệp ảnh hồ sơ, luôn là đường dẫn tuyệt đối đã chuẩn hóa. Không khai báo anh.thumuc thì dùng
     * thư mục "anh-ho-so" cạnh server.properties; đường dẫn tương đối được tính từ thư mục chứa server.properties.
     * Có thể trỏ tới thư mục chia sẻ mạng (ví dụ //MAYCHU/anh) để máy khác mở trực tiếp tệp. Trong tệp .properties
     * nên viết dấu gạch chéo xuôi, vì dấu gạch chéo ngược là ký tự thoát.
     */
    public Path anhThuMuc() {
        String configured = values.getProperty("anh.thumuc");
        try {
            Path dir = configured == null || configured.isBlank() ? baseDir.resolve("anh-ho-so") : Path.of(configured.trim());
            if (!dir.isAbsolute()) {
                dir = baseDir.resolve(dir);
            }
            return dir.toAbsolutePath().normalize();
        } catch (java.nio.file.InvalidPathException e) {
            throw new IllegalArgumentException("anh.thumuc không phải đường dẫn hợp lệ: " + configured, e);
        }
    }
}
