package com.sms.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** Chỉ số trang bắt đầu từ 0; tongPhanTu là tổng số kết quả khớp bộ lọc. */
public record TrangDTO<T extends Serializable>(
        List<T> noiDung, long tongPhanTu, int trang, int kichThuoc) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public TrangDTO {
        noiDung = List.copyOf(noiDung);
        if (tongPhanTu < 0 || trang < 0 || kichThuoc < 1
                || noiDung.size() > kichThuoc || noiDung.size() > tongPhanTu) {
            throw new IllegalArgumentException("Thông tin phân trang không hợp lệ.");
        }
    }
}

