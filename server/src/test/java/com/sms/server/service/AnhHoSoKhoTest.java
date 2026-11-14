package com.sms.server.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AnhHoSoKhoTest {

    @TempDir
    Path temporary;

    @Test
    void usesStableFileNamesAndAbsoluteNormalizedPaths() throws Exception {
        // Thư mục truyền vào có "..": đường dẫn đích vẫn là tuyệt đối và đã chuẩn hóa
        Path messy = temporary.resolve("a").resolve("..").resolve("anh");
        AnhHoSoKho kho = new AnhHoSoKho(messy);
        assertTrue(Files.isDirectory(temporary.resolve("anh")), "thư mục được tạo khi chưa có");
        Path target = kho.duongDanDich("SV001", "image/jpeg");
        assertTrue(target.isAbsolute());
        assertEquals(temporary.resolve("anh").resolve("SV001.jpg").toAbsolutePath().normalize(), target);
        assertEquals(temporary.resolve("anh").resolve("GV001.png").toAbsolutePath().normalize(),
                kho.duongDanDich("GV001", "image/png"));
        assertEquals(kho.duongDanDich("SV001", "image/jpeg"), kho.duongDanDich("SV001", "image/jpeg"));
    }

    @Test
    void unsafeUserCodesCannotEscapeTheFolderOrCollide() {
        for (String bad : new String[] {"../evil", "a/b", "a\\b", "x y", "tên", "C:\\x", ".."}) {
            String name = AnhHoSoKho.tenTep(bad, "image/jpeg");
            assertFalse(name.contains("/") || name.contains("\\") || name.contains(".."), name);
            assertTrue(name.startsWith("u"), name);
            assertTrue(name.endsWith(".jpg"));
        }
        assertNotEquals(AnhHoSoKho.tenTep("a/b", "image/png"), AnhHoSoKho.tenTep("a\\b", "image/png"));
        assertEquals("SV001.png", AnhHoSoKho.tenTep("SV001", "image/png"));
        assertEquals("sv_01-x.jpg", AnhHoSoKho.tenTep("sv_01-x", "image/jpeg"));
    }

    @Test
    void writesThroughTempFileAndReplacesAtomically() throws Exception {
        AnhHoSoKho kho = new AnhHoSoKho(temporary.resolve("kho"));
        Path target = kho.duongDanDich("SV001", "image/jpeg");

        Path first = kho.ghiTam(new byte[] {1, 2, 3});
        assertFalse(Files.exists(target), "chưa chuyển thì chưa có tệp đích");
        kho.chuyen(first, target);
        assertArrayEquals(new byte[] {1, 2, 3}, kho.doc(target));
        assertFalse(Files.exists(first), "tệp tạm đã được đổi tên");

        kho.chuyen(kho.ghiTam(new byte[] {9, 9}), target); // thay tệp cũ
        assertArrayEquals(new byte[] {9, 9}, kho.doc(target));

        try (var listing = Files.list(kho.thuMuc())) {
            assertEquals(1, listing.count(), "không để lại tệp tạm");
        }
    }

    @Test
    void readRejectsMissingAndOversizedFiles() throws Exception {
        AnhHoSoKho kho = new AnhHoSoKho(temporary.resolve("kho"));
        assertThrows(IOException.class, () -> kho.doc(kho.thuMuc().resolve("khong-co.jpg")));
        Path big = kho.thuMuc().resolve("big.jpg");
        Files.write(big, new byte[AnhHoSoValidator.TOI_DA_BYTE + 1]);
        assertThrows(IOException.class, () -> kho.doc(big));
        assertThrows(IOException.class, () -> kho.doc(kho.thuMuc()), "thư mục không phải tệp ảnh");
    }

    @Test
    void deleteIsQuietWhenFileIsMissingOrPathIsBroken() throws Exception {
        AnhHoSoKho kho = new AnhHoSoKho(temporary.resolve("kho"));
        Path target = kho.duongDanDich("SV001", "image/jpeg");
        kho.chuyen(kho.ghiTam(new byte[] {1}), target);
        kho.xoaTheoTen(target.getFileName().toString());
        assertFalse(Files.exists(target));
        assertDoesNotThrow(() -> kho.xoaTheoTen(target.getFileName().toString()));   // đã xóa rồi
        assertDoesNotThrow(() -> kho.xoaTheoTen(null));
        assertDoesNotThrow(() -> kho.xoaTheoTen("  "));
        assertDoesNotThrow(() -> kho.xoaTheoTen("\0hỏng"));             // đường dẫn không hợp lệ
    }

    @Test
    void storedValueIsRelativeFileNameResolvedInsideTheFolder() throws Exception {
        AnhHoSoKho kho = new AnhHoSoKho(temporary.resolve("kho"));
        Path target = kho.duongDanDich("SV001", "image/jpeg");
        assertEquals("SV001.jpg", AnhHoSoKho.tenLuu(target));
        assertEquals(target, kho.giaiDuongDan("SV001.jpg"));
        assertTrue(kho.giaiDuongDan("SV001.jpg").isAbsolute());

        // Giá trị bị sửa trong cơ sở dữ liệu cũng không thoát khỏi thư mục ảnh
        for (String bad : new String[] {"../x.jpg", "..\\x.jpg", "a/b.jpg", "C:\\Windows\\win.ini", "/etc/passwd",
                "//maychu/chiase/x.jpg", "..", ".", "", "  ", "x\0.jpg"}) {
            assertThrows(IOException.class, () -> kho.giaiDuongDan(bad), "'" + bad + "'");
        }
        assertThrows(IOException.class, () -> kho.giaiDuongDan(null));
        assertThrows(IOException.class, () -> kho.giaiDuongDan(target.toString()), "đường dẫn tuyệt đối không được nhận");
    }

    @Test
    void movingTheFolderKeepsStoredNamesValid() throws Exception {
        // Cùng tên tệp lưu trong DB, thư mục ảnh đổi sang nơi khác: vẫn đọc được
        AnhHoSoKho first = new AnhHoSoKho(temporary.resolve("o-cu"));
        first.chuyen(first.ghiTam(new byte[] {5, 6}), first.duongDanDich("SV001", "image/jpeg"));
        Path copied = Files.copy(first.duongDanDich("SV001", "image/jpeg"),
                Files.createDirectories(temporary.resolve("o-moi")).resolve("SV001.jpg"));
        AnhHoSoKho second = new AnhHoSoKho(copied.getParent());
        assertArrayEquals(new byte[] {5, 6}, second.doc(second.giaiDuongDan("SV001.jpg")));
    }

    @Test
    void failsFastWhenFolderCannotBeCreated() throws Exception {
        Path file = temporary.resolve("la-mot-tep");
        Files.writeString(file, "x");
        assertThrows(IOException.class, () -> new AnhHoSoKho(file.resolve("anh"))); // cha là tệp, không tạo được
    }
}
