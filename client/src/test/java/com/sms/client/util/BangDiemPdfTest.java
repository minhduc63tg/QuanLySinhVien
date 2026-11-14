package com.sms.client.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.sms.common.dto.BangDiemDTO;
import com.sms.common.dto.BangDiemHocKyDTO;
import com.sms.common.dto.DiemDTO;
import com.sms.common.dto.SinhVienDTO;
import com.sms.common.enums.GioiTinh;
import com.sms.common.enums.TrangThaiHocTap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BangDiemPdfTest {

    private static final SinhVienDTO SV = new SinhVienDTO("SV001", "Nguyễn Văn An", LocalDate.of(2004, 1, 2),
            GioiTinh.NAM, "an@example.com", "0900000000", TrangThaiHocTap.DANG_HOC,
            "KTPM22A", "Kỹ thuật phần mềm 22A", "CNTT", "Công nghệ thông tin");

    private static DiemDTO diem(String maMon, String tenMon, int tinChi, String hocKy, Double so, String chu,
            Double he4, Boolean dat) {
        return new DiemDTO("SV001", "Nguyễn Văn An", "L-" + maMon + hocKy, maMon, tenMon, tinChi, hocKy,
                so, chu, he4, dat);
    }

    /** Học kỳ mới nhất đứng trước, đúng như server trả về. */
    private static BangDiemDTO bangDiem() {
        BangDiemHocKyDTO moi = new BangDiemHocKyDTO("HK2-2025-2026", List.of(
                diem("CTDL", "Cấu trúc dữ liệu", 3, "HK2-2025-2026", 8.5, "A", 3.8, true),
                diem("LTM", "Lập trình mạng", 3, "HK2-2025-2026", null, null, null, null)), 3.8);
        BangDiemHocKyDTO cu = new BangDiemHocKyDTO("HK1-2025-2026", List.of(
                diem("TOAN", "Toán cao cấp", 3, "HK1-2025-2026", 3.0, "F", 0.0, false),
                diem("LTCB", "Lập trình cơ bản", 4, "HK1-2025-2026", 9.0, "A+", 4.0, true)), 2.29);
        return new BangDiemDTO(List.of(moi, cu), 2.9);
    }

    private static byte[] pdf(BangDiemDTO bangDiem) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BangDiemPdf.ghi(SV, bangDiem, LocalDate.of(2026, 11, 13), out);
        return out.toByteArray();
    }

    @Test
    void taoTepPdfHopLeCoTiengVietVaDungThuTuHocKy() throws Exception {
        assumeTrue(BangDiemPdf.coPhong(), "Máy không có phông chữ tiếng Việt");
        byte[] data = pdf(bangDiem());
        assertEquals("%PDF", new String(data, 0, 4, java.nio.charset.StandardCharsets.US_ASCII));

        PdfReader reader = new PdfReader(data);
        assertTrue(reader.getNumberOfPages() >= 1);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertTrue(text.contains("BẢNG ĐIỂM SINH VIÊN"), text);
        assertTrue(text.contains("Nguyễn Văn An"), text);
        assertTrue(text.contains("Cấu trúc dữ liệu"), text);
        assertTrue(text.contains("Chưa có điểm"), text);
        assertTrue(text.contains("Không đạt"), text);
        assertTrue(text.contains("02/01/2004"), text);
        assertTrue(text.contains("GPA tích lũy (hệ 4): 2.90"), text);
        // Bảng điểm in học kỳ cũ trước, mới sau
        assertTrue(text.indexOf("HK1-2025-2026") < text.indexOf("HK2-2025-2026"), text);
    }

    @Test
    void khongCoDuLieuDangKyVanTaoDuocPdf() throws Exception {
        assumeTrue(BangDiemPdf.coPhong(), "Máy không có phông chữ tiếng Việt");
        byte[] data = pdf(new BangDiemDTO(List.of(), null));
        String text = new PdfTextExtractor(new PdfReader(data)).getTextFromPage(1);
        assertTrue(text.contains("Chưa có dữ liệu đăng ký học phần"), text);
        assertTrue(text.contains("GPA tích lũy (hệ 4): chưa có"), text);
    }

    @Test
    void nhieuMonThiNhieuTrangVanDungDuThuTu() throws Exception {
        assumeTrue(BangDiemPdf.coPhong(), "Máy không có phông chữ tiếng Việt");
        List<DiemDTO> many = new java.util.ArrayList<>();
        for (int i = 1; i <= 80; i++) {
            many.add(diem("M" + i, "Môn học số " + i, 3, "HK1-2025-2026", 7.0, "B", 3.0, true));
        }
        byte[] data = pdf(new BangDiemDTO(List.of(new BangDiemHocKyDTO("HK1-2025-2026", many, 3.0)), 3.0));
        PdfReader reader = new PdfReader(data);
        assertTrue(reader.getNumberOfPages() >= 2);
        String last = new PdfTextExtractor(reader).getTextFromPage(reader.getNumberOfPages());
        assertTrue(last.contains("Tổng số tín chỉ đã đạt: 240"), last);
    }

    @Test
    void tinChiDaDatChiTinhMotLanChoMonHocLaiVaBoQuaMonKhongDat() {
        BangDiemHocKyDTO ky1 = new BangDiemHocKyDTO("HK1", List.of(
                diem("A", "A", 3, "HK1", 3.0, "F", 0.0, false),
                diem("B", "B", 2, "HK1", 8.0, "B+", 3.5, true)), 1.4);
        BangDiemHocKyDTO ky2 = new BangDiemHocKyDTO("HK2", List.of(
                diem("A", "A", 3, "HK2", 6.0, "C+", 2.5, true),
                diem("B", "B", 2, "HK2", 9.0, "A+", 4.0, true),
                diem("C", "C", 1, "HK2", null, null, null, null)), 3.0);
        assertEquals(5, BangDiemPdf.tinChiDaDat(new BangDiemDTO(List.of(ky2, ky1), 2.0)));
    }

    @Test
    void tenTepKhongChuaKyTuLa() {
        assertEquals("BangDiem_SV001.pdf", BangDiemPdf.tenTep(SV));
        SinhVienDTO la = new SinhVienDTO("a/b\\c:d", "X", null, null, null, null, TrangThaiHocTap.DANG_HOC,
                null, null, null, null);
        assertEquals("BangDiem_a_b_c_d.pdf", BangDiemPdf.tenTep(la));
    }

    @Test
    void baoLoiRoRangKhiPhongChuChiDinhKhongDocDuoc() {
        String old = System.getProperty("sms.pdf.font");
        System.setProperty("sms.pdf.font", "khong-ton-tai.ttf");
        try {
            IOException error = assertThrows(IOException.class, () -> pdf(bangDiem()));
            assertTrue(error.getMessage().contains("phông chữ"), error.getMessage());
        } finally {
            if (old == null) {
                System.clearProperty("sms.pdf.font");
            } else {
                System.setProperty("sms.pdf.font", old);
            }
        }
    }
}
