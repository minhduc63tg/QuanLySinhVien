/**
 * Hợp đồng RMI giữa client và server.
 *
 * <p>Ngoại trừ đăng nhập, tham số đầu tiên luôn là token. Server phải xác thực
 * token và kiểm tra vai trò ở mỗi lời gọi; các method "CuaToi" và cập nhật liên hệ
 * lấy mã người dùng từ session. Kiểm tra quyền và nghiệp vụ được triển khai tại server.
 *
 * <p>DTO chỉ truyền dữ liệu, không truyền entity. Khi ghi, server chỉ nhận các trường
 * nghiệp vụ được phép sửa; tên liên quan, sĩ số đã đăng ký, version và điểm tổng hợp
 * do server quản lý. Môn tiên quyết cập nhật qua capNhatTienQuyet. Có thể gửi lịch
 * khi tạo lớp học phần; cập nhật lịch của lớp đã có qua luuLichHoc.
 * Tạo/sửa hồ sơ không nhận mật khẩu; tạo tài khoản/reset mật khẩu do server xử lý.
 *
 * <p>Phân trang tính từ 0. Bộ lọc null hoặc chuỗi trống nghĩa là không lọc.
 * Ghi điểm hàng loạt phải kiểm tra toàn bộ dữ liệu và lưu trong một transaction;
 * null không phải yêu cầu xóa điểm. Client truyền collection có thể serialize
 * (ví dụ ArrayList, HashSet, HashMap hoặc collection từ List/Set/Map.copyOf).
 */
package com.sms.common.rmi;

