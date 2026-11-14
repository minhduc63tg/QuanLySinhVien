package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Kiểm tra dữ liệu ảnh hồ sơ do client gửi (không tin client): kích thước, định dạng theo nội dung và
 * số điểm ảnh. Chỉ đọc phần đầu tệp để lấy kích thước, không giải mã cả ảnh nên chống được ảnh "bom".
 */
final class AnhHoSoValidator {

    static final int TOI_DA_BYTE = 1024 * 1024;
    static final int TOI_DA_CANH = 2000;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private AnhHoSoValidator() {
    }

    /** Trả về kiểu nội dung (image/jpeg hoặc image/png) nếu hợp lệ. */
    static String kiemTra(byte[] data) throws BusinessException {
        ServiceSupport.check(data != null && data.length > 0, "Dữ liệu ảnh không được trống");
        ServiceSupport.check(data.length <= TOI_DA_BYTE, "Ảnh không được lớn hơn 1 MB");
        String type = kieu(data);
        ServiceSupport.check(type != null, "Chỉ chấp nhận ảnh JPEG hoặc PNG");
        int width;
        int height;
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "Không đọc được ảnh");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, true, true);
                width = reader.getWidth(0);
                height = reader.getHeight(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw new BusinessException(ErrorCode.DU_LIEU_KHONG_HOP_LE, "Không đọc được ảnh");
        }
        ServiceSupport.check(width > 0 && height > 0 && width <= TOI_DA_CANH && height <= TOI_DA_CANH,
                "Kích thước ảnh tối đa " + TOI_DA_CANH + "x" + TOI_DA_CANH + " điểm ảnh");
        return type;
    }

    /** Nhận dạng theo chữ ký đầu tệp, không tin tên tệp hay kiểu do client khai báo. */
    static String kieu(byte[] data) {
        if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data.length >= PNG.length) {
            for (int i = 0; i < PNG.length; i++) {
                if (data[i] != PNG[i]) {
                    return null;
                }
            }
            return "image/png";
        }
        return null;
    }
}
