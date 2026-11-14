package com.sms.client.util;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sms.common.dto.BangDiemDTO;
import com.sms.common.dto.BangDiemHocKyDTO;
import com.sms.common.dto.DiemDTO;
import com.sms.common.dto.SinhVienDTO;
import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Dựng bảng điểm sinh viên thành tệp PDF (A4 dọc, học kỳ cũ trước mới sau). Không phụ thuộc JavaFX
 * để kiểm thử được. Phông chữ tiếng Việt lấy từ phông có sẵn trên máy (Times New Roman, Arial,
 * Liberation, DejaVu...), hoặc từ tệp .ttf chỉ định bằng {@code -Dsms.pdf.font=...}
 * (và tùy chọn {@code -Dsms.pdf.font.bold=...}).
 */
public final class BangDiemPdf {

    /** Phông thường và phông đậm đã nhúng vào PDF. */
    record Phong(BaseFont thuong, BaseFont dam) {
    }

    /** Cặp (thường, đậm) theo thứ tự ưu tiên tìm trên máy. */
    private static final List<String[]> UNG_VIEN = List.of(
            new String[] {"C:/Windows/Fonts/times.ttf", "C:/Windows/Fonts/timesbd.ttf"},
            new String[] {"C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/arialbd.ttf"},
            new String[] {"/System/Library/Fonts/Supplemental/Times New Roman.ttf",
                    "/System/Library/Fonts/Supplemental/Times New Roman Bold.ttf"},
            new String[] {"/Library/Fonts/Times New Roman.ttf", "/Library/Fonts/Times New Roman Bold.ttf"},
            new String[] {"/usr/share/fonts/truetype/liberation/LiberationSerif-Regular.ttf",
                    "/usr/share/fonts/truetype/liberation/LiberationSerif-Bold.ttf"},
            new String[] {"/usr/share/fonts/liberation/LiberationSerif-Regular.ttf",
                    "/usr/share/fonts/liberation/LiberationSerif-Bold.ttf"},
            new String[] {"/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf",
                    "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"},
            new String[] {"/usr/share/fonts/TTF/DejaVuSerif.ttf", "/usr/share/fonts/TTF/DejaVuSerif-Bold.ttf"});

    private static final Color NEN_TIEU_DE = new Color(0xE5, 0xE7, 0xEB);

    private BangDiemPdf() {
    }

    /** Tên tệp gợi ý, ví dụ {@code BangDiem_SV001.pdf}. */
    public static String tenTep(SinhVienDTO sinhVien) {
        return "BangDiem_" + sinhVien.maSv().replaceAll("[^A-Za-z0-9_-]", "_") + ".pdf";
    }

    /** Có tìm được phông chữ để dựng PDF trên máy này không. */
    public static boolean coPhong() {
        return timPhong().isPresent();
    }

    /** Ghi bảng điểm ra {@code out}; không đóng {@code out}. */
    public static void ghi(SinhVienDTO sinhVien, BangDiemDTO bangDiem, LocalDate ngayIn, OutputStream out)
            throws IOException {
        Phong phong = timPhong().orElseThrow(() -> new IOException(
                "Không tìm thấy phông chữ hỗ trợ tiếng Việt để tạo PDF. "
                        + "Hãy chạy client với -Dsms.pdf.font=<đường dẫn tệp .ttf>."));
        Font thuong = new Font(phong.thuong(), 11);
        Font dam = new Font(phong.dam(), 11);
        Font nho = new Font(phong.thuong(), 10);
        Font nhoDam = new Font(phong.dam(), 10);

        Document document = new Document(PageSize.A4, 54, 54, 50, 56);
        try {
            PdfWriter.getInstance(document, out);
            HeaderFooter footer = new HeaderFooter(new Phrase("Trang ", nho), true);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setBorder(Rectangle.NO_BORDER);
            document.setFooter(footer);
            document.addTitle("Bảng điểm " + sinhVien.maSv());
            document.addCreator("Hệ thống quản lý sinh viên");
            document.open();

            Paragraph title = new Paragraph("BẢNG ĐIỂM SINH VIÊN", new Font(phong.dam(), 18));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(14);
            document.add(title);

            document.add(thongTinSinhVien(sinhVien, thuong, dam));

            List<BangDiemHocKyDTO> hocKy = new ArrayList<>(bangDiem.hocKy());
            java.util.Collections.reverse(hocKy); // DTO trả mới nhất trước; bảng điểm in cũ trước
            if (hocKy.isEmpty()) {
                Paragraph empty = new Paragraph("Chưa có dữ liệu đăng ký học phần.", thuong);
                empty.setSpacingBefore(12);
                document.add(empty);
            }
            for (BangDiemHocKyDTO ky : hocKy) {
                Paragraph heading = new Paragraph("Học kỳ: " + ky.hocKy(), dam);
                heading.setSpacingBefore(14);
                heading.setSpacingAfter(7);
                heading.setKeepTogether(true);
                document.add(heading);
                document.add(bangHocKy(ky, nho, nhoDam));
                Paragraph gpa = new Paragraph("GPA học kỳ (hệ 4): " + Formatters.gpa(ky.gpa()), nhoDam);
                gpa.setAlignment(Element.ALIGN_RIGHT);
                gpa.setSpacingBefore(3);
                document.add(gpa);
            }

            Paragraph tong = new Paragraph();
            tong.setSpacingBefore(18);
            tong.add(new Phrase("GPA tích lũy (hệ 4): " + Formatters.gpa(bangDiem.gpaTichLuy()) + "\n", dam));
            tong.add(new Phrase("Tổng số tín chỉ đã đạt: " + tinChiDaDat(bangDiem) + "\n", thuong));
            tong.add(new Phrase("Ngày in: " + Formatters.date(ngayIn), thuong));
            tong.setKeepTogether(true);
            document.add(tong);
        } catch (DocumentException e) {
            throw new IOException("Không tạo được tệp PDF: " + e.getMessage(), e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /** Tổng tín chỉ của các môn đã đạt; môn học lại chỉ tính một lần. */
    static int tinChiDaDat(BangDiemDTO bangDiem) {
        Set<String> daTinh = new HashSet<>();
        int total = 0;
        for (BangDiemHocKyDTO ky : bangDiem.hocKy()) {
            for (DiemDTO diem : ky.bangDiem()) {
                if (Boolean.TRUE.equals(diem.dat()) && daTinh.add(diem.maMon())) {
                    total += diem.soTinChi();
                }
            }
        }
        return total;
    }

    private static PdfPTable thongTinSinhVien(SinhVienDTO sv, Font thuong, Font dam) {
        PdfPTable table = new PdfPTable(new float[] {1.3f, 2.2f, 1.1f, 2.0f});
        table.setWidthPercentage(100);
        String[][] rows = {
                {"Mã sinh viên:", sv.maSv(), "Giới tính:", sv.gioiTinh() == null ? "" : Formatters.gioiTinh(sv.gioiTinh())},
                {"Họ và tên:", sv.hoTen(), "Ngày sinh:", Formatters.date(sv.ngaySinh())},
                {"Lớp:", nullToEmpty(sv.tenLop()), "Khoa:", nullToEmpty(sv.tenKhoa())},
                {"Tình trạng:", Formatters.trangThai(sv.trangThai()), "", ""},
        };
        for (String[] row : rows) {
            for (int i = 0; i < 4; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(row[i], i % 2 == 0 ? thuong : dam));
                cell.setBorder(Rectangle.NO_BORDER);
                cell.setPaddingBottom(3);
                table.addCell(cell);
            }
        }
        return table;
    }

    private static PdfPTable bangHocKy(BangDiemHocKyDTO ky, Font thuong, Font dam) {
        PdfPTable table = new PdfPTable(new float[] {0.85f, 1.3f, 3.4f, 0.6f, 1.1f, 1.1f, 1.1f, 1.5f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6); // chừa khoảng giữa tiêu đề học kỳ và bảng
        table.setHeaderRows(1);
        String[] heads = {"STT", "Mã môn", "Tên môn", "TC", "Điểm (10)", "Điểm chữ", "Điểm (4)", "Đánh giá"};
        for (String head : heads) {
            PdfPCell cell = new PdfPCell(new Phrase(head, dam));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setBackgroundColor(NEN_TIEU_DE);
            cell.setPadding(4);
            table.addCell(cell);
        }
        int stt = 1;
        for (DiemDTO diem : ky.bangDiem()) {
            String[] values = {
                    String.valueOf(stt++), diem.maMon(), diem.tenMon(), String.valueOf(diem.soTinChi()),
                    Formatters.diem(diem.diemSo()), nullToEmpty(diem.diemChu()),
                    diem.diemHe4() == null ? "" : String.format(java.util.Locale.ROOT, "%.1f", diem.diemHe4()),
                    diem.dat() == null ? "Chưa có điểm" : diem.dat() ? "Đạt" : "Không đạt"};
            for (int i = 0; i < values.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(values[i], thuong));
                cell.setPadding(4);
                cell.setHorizontalAlignment(i == 2 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
                table.addCell(cell);
            }
        }
        return table;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Optional<Phong> timPhong() {
        String chiDinh = System.getProperty("sms.pdf.font");
        if (chiDinh != null && !chiDinh.isBlank()) {
            String dam = System.getProperty("sms.pdf.font.bold");
            return nap(chiDinh.trim(), dam == null || dam.isBlank() ? chiDinh.trim() : dam.trim());
        }
        for (String[] cap : UNG_VIEN) {
            if (Files.isRegularFile(Path.of(cap[0])) && Files.isRegularFile(Path.of(cap[1]))) {
                Optional<Phong> found = nap(cap[0], cap[1]);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<Phong> nap(String thuong, String dam) {
        try {
            return Optional.of(new Phong(
                    BaseFont.createFont(thuong, BaseFont.IDENTITY_H, BaseFont.EMBEDDED),
                    BaseFont.createFont(dam, BaseFont.IDENTITY_H, BaseFont.EMBEDDED)));
        } catch (DocumentException | IOException e) {
            return Optional.empty();
        }
    }
}
