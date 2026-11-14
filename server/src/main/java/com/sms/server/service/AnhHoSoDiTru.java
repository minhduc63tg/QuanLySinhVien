package com.sms.server.service;

import com.sms.server.jpa.JpaUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Đưa dữ liệu ảnh hồ sơ kiểu cũ về kiểu hiện tại (tệp trong thư mục anh.thumuc, cơ sở dữ liệu chỉ giữ tên tệp).
 * Hai bước, đều idempotent và chỉ chạy khi khởi động server thật:
 * <ol>
 *   <li>Ảnh lưu thẳng trong cột du_lieu: ghi ra tệp, ghi tên tệp vào duong_dan, rồi bỏ cột du_lieu.</li>
 *   <li>duong_dan còn là đường dẫn tuyệt đối (phiên bản trước): đổi thành tên tệp, chép tệp vào thư mục ảnh nếu
 *       nó đang nằm ở nơi khác.</li>
 * </ol>
 * Ảnh nào không xử lý được thì giữ nguyên dữ liệu cũ để không mất và thử lại ở lần khởi động sau.
 */
@Slf4j
public final class AnhHoSoDiTru {

    private AnhHoSoDiTru() {
    }

    public static void chay(JpaUtil jpa, AnhHoSoKho kho) {
        chuyenCotCu(jpa, kho);
        doiDuongDanTuyetDoi(jpa, kho);
    }

    private static void chuyenCotCu(JpaUtil jpa, AnhHoSoKho kho) {
        boolean legacy = jpa.query(em -> ((Number) em.createNativeQuery("""
                select count(*) from information_schema.columns
                where table_schema = database() and table_name = 'anh_ho_so' and column_name = 'du_lieu'
                """).getSingleResult()).longValue() > 0);
        if (!legacy) {
            return;
        }
        @SuppressWarnings("unchecked")
        List<Object[]> rows = jpa.query(em -> em.createNativeQuery(
                "select ten_dang_nhap, kieu_noi_dung, du_lieu from anh_ho_so where du_lieu is not null")
                .getResultList());
        log.info("Chuyển {} ảnh hồ sơ từ cơ sở dữ liệu ra tệp trong {}", rows.size(), kho.thuMuc());
        boolean allMoved = true;
        for (Object[] row : rows) {
            String user = (String) row[0];
            String type = (String) row[1];
            try {
                Path target = kho.duongDanDich(user, type);
                Path temp = kho.ghiTam(bytes(row[2]));
                try {
                    kho.chuyen(temp, target);
                } finally {
                    Files.deleteIfExists(temp);
                }
                capNhatTen(jpa, user, AnhHoSoKho.tenLuu(target));
            } catch (IOException | SQLException | RuntimeException e) {
                allMoved = false;
                log.error("Không chuyển được ảnh của {}", user, e);
            }
        }
        if (allMoved) {
            jpa.inTransaction(em -> em.createNativeQuery("alter table anh_ho_so drop column du_lieu").executeUpdate());
            log.info("Đã chuyển xong ảnh hồ sơ ra tệp và bỏ cột du_lieu.");
        }
    }

    private static void doiDuongDanTuyetDoi(JpaUtil jpa, AnhHoSoKho kho) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = jpa.query(em -> em.createNativeQuery(
                "select ten_dang_nhap, kieu_noi_dung, duong_dan from anh_ho_so where duong_dan <> ''")
                .getResultList());
        for (Object[] row : rows) {
            String user = (String) row[0];
            String type = (String) row[1];
            String stored = (String) row[2];
            if (laTenTep(stored)) {
                continue;
            }
            try {
                Path target = kho.duongDanDich(user, type);
                Path old = Path.of(stored).toAbsolutePath().normalize();
                if (!old.equals(target) && Files.isRegularFile(old)) {
                    // Tệp đang nằm ngoài thư mục ảnh hiện tại: chép vào (giữ nguyên bản gốc)
                    Path temp = kho.ghiTam(Files.readAllBytes(old));
                    try {
                        kho.chuyen(temp, target);
                    } finally {
                        Files.deleteIfExists(temp);
                    }
                    log.info("Đã chép ảnh của {} từ {} vào {}", user, old, target);
                }
                capNhatTen(jpa, user, AnhHoSoKho.tenLuu(target));
                log.info("Ảnh của {}: đường dẫn tuyệt đối đổi thành tên tệp {}", user, AnhHoSoKho.tenLuu(target));
            } catch (IOException | RuntimeException e) { // gồm cả InvalidPathException
                log.error("Không đổi được đường dẫn ảnh của {} ({})", user, stored, e);
            }
        }
    }

    /** Giá trị đã là tên tệp đơn (không có dấu phân cách thư mục). */
    static boolean laTenTep(String value) {
        return !value.isBlank() && value.indexOf('/') < 0 && value.indexOf('\\') < 0 && value.indexOf(':') < 0;
    }

    private static void capNhatTen(JpaUtil jpa, String user, String name) {
        jpa.inTransaction(em -> em.createNativeQuery(
                "update anh_ho_so set duong_dan = ?1 where ten_dang_nhap = ?2")
                .setParameter(1, name).setParameter(2, user).executeUpdate());
    }

    private static byte[] bytes(Object value) throws SQLException {
        if (value instanceof byte[] data) {
            return data;
        }
        if (value instanceof Blob blob) {
            return blob.getBytes(1, (int) blob.length());
        }
        throw new SQLException("Kiểu dữ liệu ảnh không nhận dạng được: " + value);
    }
}
