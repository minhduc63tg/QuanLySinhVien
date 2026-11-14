package com.sms.server.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ảnh hồ sơ của một tài khoản sinh viên hoặc giảng viên (mỗi tài khoản tối đa một ảnh). Chỉ lưu đường dẫn
 * tương đối (tên tệp) tới ảnh trên máy chủ; nội dung ảnh nằm ngoài cơ sở dữ liệu.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "anh_ho_so")
public class AnhHoSo {

    @Id
    @Column(name = "ten_dang_nhap", length = 20, nullable = false)
    private String tenDangNhap;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "ten_dang_nhap")
    private TaiKhoan taiKhoan;

    /** image/jpeg hoặc image/png, xác định từ nội dung chứ không tin tên tệp. */
    @Column(name = "kieu_noi_dung", length = 20, nullable = false)
    private String kieuNoiDung;

    /**
     * Đường dẫn tương đối của tệp ảnh, tính từ thư mục anh.thumuc: chỉ là tên tệp, ví dụ "SV001.jpg". Không lưu
     * đường dẫn tuyệt đối để đổi ổ đĩa hoặc chuyển server sang máy khác chỉ cần chép thư mục ảnh.
     */
    @Column(name = "duong_dan", length = 500, nullable = false, columnDefinition = "varchar(500) not null default ''")
    private String duongDan;

    @Column(name = "cap_nhat", nullable = false)
    private LocalDateTime capNhat;

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof AnhHoSo entity
                && getTenDangNhap() != null && getTenDangNhap().equals(entity.getTenDangNhap());
    }

    @Override
    public final int hashCode() {
        return AnhHoSo.class.hashCode();
    }
}
