package com.sms.common;

import com.sms.common.dto.*;
import com.sms.common.enums.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SerializationTest {

    @Test
    void nestedRecordsDatesEnumsAndNullGradesSurviveSerialization() throws Exception {
        LopHocPhanDTO lop = new LopHocPhanDTO("CTDL-01", "CTDL",
                "Cấu trúc dữ liệu", 3, "GV001", "Nguyễn Minh Anh",
                "HK1-2026-2027", 40, 10, 1,
                List.of(new LichHocDTO(1L, 2, 1, 3, "A101")));
        SinhVienDTO sv = new SinhVienDTO("SV001", "Nguyễn Minh Khang",
                LocalDate.of(2004, 1, 1), GioiTinh.NAM, null, null,
                TrangThaiHocTap.DANG_HOC, "DHKTPM22A", "Kỹ thuật phần mềm 22A",
                "CNTT", "Công nghệ thông tin");
        DiemDTO chuaCoDiem = new DiemDTO("SV001", sv.hoTen(), lop.maLhp(),
                lop.maMon(), lop.tenMon(), 3, lop.hocKy(), null, null, null, null);
        List<Serializable> payloads = List.of(
                new TrangDTO<>(List.of(sv), 1, 0, 20),
                new DangKyDTO(lop, LocalDateTime.of(2026, 11, 12, 9, 30), null),
                new LopCoTheDangKyDTO(lop, false, ErrorCode.DA_DANG_KY, "Đã đăng ký lớp này"),
                new BangDiemDTO(List.of(new BangDiemHocKyDTO(
                        lop.hocKy(), List.of(chuaCoDiem), null)), null),
                new MonHocDTO("CTDL", "Cấu trúc dữ liệu", 3, Set.of("OOP")),
                new ThoiKhoaBieuDTO(lop.hocKy(), List.of(new TietHocDTO(
                        lop.maLhp(), lop.maMon(), lop.tenMon(), lop.maGv(),
                        lop.tenGiangVien(), 2, 1, 3, "A101"))));
        for (Serializable payload : payloads) {
            assertEquals(payload, roundTrip(payload));
        }
    }

    @Test
    void collectionSnapshotsRemainImmutableAfterSerialization() throws Exception {
        ArrayList<KhoaDTO> original = new ArrayList<>(List.of(
                new KhoaDTO("CNTT", "Công nghệ thông tin")));
        TrangDTO<KhoaDTO> page = new TrangDTO<>(original, 1, 0, 20);
        original.clear();
        assertEquals(1, page.noiDung().size());
        assertThrows(UnsupportedOperationException.class, () -> page.noiDung().clear());
        TrangDTO<?> copy = (TrangDTO<?>) roundTrip(page);
        assertThrows(UnsupportedOperationException.class, () -> copy.noiDung().clear());

        HashSet<String> prerequisites = new HashSet<>(Set.of("OOP"));
        MonHocDTO mon = new MonHocDTO("CTDL", "Cấu trúc dữ liệu", 3, prerequisites);
        prerequisites.clear();
        assertEquals(Set.of("OOP"), mon.maMonTienQuyet());
        assertThrows(UnsupportedOperationException.class, () -> mon.maMonTienQuyet().clear());
    }

    @Test
    void businessErrorRetainsCodeAndVietnameseMessage() throws Exception {
        BusinessException error = new BusinessException(ErrorCode.TRUNG_LICH, "Trùng lịch học");
        BusinessException copy = (BusinessException) roundTrip(error);
        assertEquals(error.getCode(), copy.getCode());
        assertEquals(error.getMessage(), copy.getMessage());
    }

    private static Object roundTrip(Serializable value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return input.readObject();
        }
    }
}
