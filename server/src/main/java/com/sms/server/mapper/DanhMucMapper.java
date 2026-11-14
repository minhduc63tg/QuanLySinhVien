package com.sms.server.mapper;

import com.sms.common.dto.KhoaDTO;
import com.sms.common.dto.LopHocDTO;
import com.sms.common.dto.MonHocDTO;
import com.sms.server.entity.Khoa;
import com.sms.server.entity.LopHoc;
import com.sms.server.entity.MonHoc;
import java.util.stream.Collectors;

/** Gọi trong transaction, sau khi đã fetch quan hệ cần thiết. */
public final class DanhMucMapper {

    private DanhMucMapper() {
    }

    public static KhoaDTO khoa(Khoa value) {
        return new KhoaDTO(value.getMaKhoa(), value.getTenKhoa());
    }

    public static LopHocDTO lop(LopHoc value) {
        return new LopHocDTO(value.getMaLop(), value.getTenLop(),
                value.getKhoa().getMaKhoa(), value.getKhoa().getTenKhoa());
    }

    public static MonHocDTO mon(MonHoc value) {
        return new MonHocDTO(value.getMaMon(), value.getTenMon(), value.getSoTinChi(),
                value.getMonTienQuyet().stream().map(MonHoc::getMaMon).collect(Collectors.toSet()));
    }
}
