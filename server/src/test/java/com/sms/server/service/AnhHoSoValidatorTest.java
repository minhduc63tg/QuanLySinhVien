package com.sms.server.service;

import com.sms.common.BusinessException;
import com.sms.common.enums.ErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnhHoSoValidatorTest {

    static byte[] image(String format, int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, format, out));
        return out.toByteArray();
    }

    private static void rejects(byte[] data) {
        BusinessException error = assertThrows(BusinessException.class, () -> AnhHoSoValidator.kiemTra(data));
        assertEquals(ErrorCode.DU_LIEU_KHONG_HOP_LE, error.getCode());
    }

    @Test
    void acceptsJpegAndPngAndDetectsTypeFromContent() throws Exception {
        assertEquals("image/png", AnhHoSoValidator.kiemTra(image("png", 30, 40)));
        assertEquals("image/jpeg", AnhHoSoValidator.kiemTra(image("jpg", 30, 40)));
        assertEquals("image/png", AnhHoSoValidator.kiemTra(image("png", AnhHoSoValidator.TOI_DA_CANH, 10)));
    }

    @Test
    void rejectsEmptyOrNull() {
        rejects(null);
        rejects(new byte[0]);
    }

    @Test
    void rejectsOtherFormatsEvenIfTheyAreImages() throws Exception {
        rejects(image("gif", 10, 10));
        rejects(image("bmp", 10, 10));
        rejects("<html>not an image</html>".getBytes());
        rejects(new byte[] {(byte) 0xFF, (byte) 0xD8}); // quá ngắn để là JPEG
    }

    @Test
    void rejectsCorruptContentWithValidSignature() {
        byte[] fake = new byte[200];
        Arrays.fill(fake, (byte) 7);
        fake[0] = (byte) 0xFF;
        fake[1] = (byte) 0xD8;
        fake[2] = (byte) 0xFF;
        rejects(fake);
    }

    @Test
    void rejectsTooLargeFileOrDimensions() throws Exception {
        byte[] big = new byte[AnhHoSoValidator.TOI_DA_BYTE + 1];
        big[0] = (byte) 0xFF;
        big[1] = (byte) 0xD8;
        big[2] = (byte) 0xFF;
        rejects(big);
        rejects(image("png", AnhHoSoValidator.TOI_DA_CANH + 1, 10));
        rejects(image("png", 10, AnhHoSoValidator.TOI_DA_CANH + 1));
    }
}
