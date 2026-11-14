package com.sms.client.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Chuẩn hóa ảnh hồ sơ trước khi gửi lên server: cắt giữa theo tỉ lệ 3:4 (ảnh chân dung), thu về 300x400
 * và nén JPEG. Nhờ vậy ảnh chụp từ điện thoại vài MB chỉ còn khoảng vài chục KB, luôn dưới giới hạn 1 MB
 * của server.
 */
public final class AnhHoSoUtil {

    public static final int RONG = 300;
    public static final int CAO = 400;

    private static final long TOI_DA_TEP = 30L * 1024 * 1024;
    private static final float CHAT_LUONG = 0.88f;

    private AnhHoSoUtil() {
    }

    /** Đọc tệp ảnh (JPG, PNG, GIF, BMP), cắt/thu nhỏ rồi trả về dữ liệu JPEG. Chạy nền, không chạy trên FX thread. */
    public static byte[] chuanHoa(File file) throws IOException {
        if (file.length() > TOI_DA_TEP) {
            throw new IOException("Tệp ảnh quá lớn (tối đa 30 MB).");
        }
        BufferedImage source = ImageIO.read(file);
        if (source == null) {
            throw new IOException("Không đọc được ảnh. Hãy chọn tệp JPG, PNG, GIF hoặc BMP.");
        }
        int w = source.getWidth();
        int h = source.getHeight();
        double target = (double) RONG / CAO;
        int cropW = w;
        int cropH = h;
        if ((double) w / h > target) {
            cropW = (int) Math.round(h * target);
        } else {
            cropH = (int) Math.round(w / target);
        }
        int x = (w - cropW) / 2;
        int y = (h - cropH) / 2;

        BufferedImage out = new BufferedImage(RONG, CAO, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setColor(Color.WHITE); // ảnh PNG trong suốt được đặt lên nền trắng
            g.fillRect(0, 0, RONG, CAO);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, RONG, CAO, x, y, x + cropW, y + cropH, null);
        } finally {
            g.dispose();
        }
        return jpeg(out);
    }

    private static byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(CHAT_LUONG);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(bytes)) {
                writer.setOutput(stream);
                writer.write(null, new IIOImage(image, null, null), param);
            }
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
