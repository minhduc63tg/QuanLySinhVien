package com.sms.client.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AnhHoSoUtilTest {

    @TempDir
    Path temporary;

    private File write(String name, BufferedImage image, String format) throws Exception {
        File file = temporary.resolve(name).toFile();
        assertTrue(ImageIO.write(image, format, file));
        return file;
    }

    private static BufferedImage solid(int w, int h, int type, Color color) {
        BufferedImage image = new BufferedImage(w, h, type);
        var g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return image;
    }

    private static BufferedImage decode(byte[] data) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(data));
    }

    @Test
    void landscapePhotoIsCroppedAndScaledToPortraitJpeg() throws Exception {
        byte[] result = AnhHoSoUtil.chuanHoa(write("a.png", solid(4000, 3000, BufferedImage.TYPE_INT_RGB, Color.RED), "png"));
        BufferedImage out = decode(result);
        assertEquals(AnhHoSoUtil.RONG, out.getWidth());
        assertEquals(AnhHoSoUtil.CAO, out.getHeight());
        assertEquals((byte) 0xFF, result[0]); // chữ ký JPEG mà server chấp nhận
        assertEquals((byte) 0xD8, result[1]);
        assertTrue(result.length < 1024 * 1024);
    }

    @Test
    void portraitAndTinyPhotosAlsoProduceExactSize() throws Exception {
        for (int[] size : new int[][] {{600, 1200}, {30, 40}, {500, 500}}) {
            BufferedImage out = decode(AnhHoSoUtil.chuanHoa(write("p" + size[0] + ".png",
                    solid(size[0], size[1], BufferedImage.TYPE_INT_RGB, Color.BLUE), "png")));
            assertEquals(AnhHoSoUtil.RONG, out.getWidth());
            assertEquals(AnhHoSoUtil.CAO, out.getHeight());
        }
    }

    @Test
    void cropKeepsTheCenterOfTheImage() throws Exception {
        // Ảnh ngang: nửa trái đỏ, nửa phải xanh, dải giữa trắng; sau khi cắt giữa dải trắng vẫn ở giữa
        BufferedImage wide = new BufferedImage(1200, 400, BufferedImage.TYPE_INT_RGB);
        var g = wide.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 600, 400);
        g.setColor(Color.BLUE);
        g.fillRect(600, 0, 600, 400);
        g.dispose();
        BufferedImage out = decode(AnhHoSoUtil.chuanHoa(write("w.png", wide, "png")));
        Color left = new Color(out.getRGB(10, 200));
        Color right = new Color(out.getRGB(AnhHoSoUtil.RONG - 10, 200));
        assertTrue(left.getRed() > 150 && left.getBlue() < 100, "mép trái phải là đỏ");
        assertTrue(right.getBlue() > 150 && right.getRed() < 100, "mép phải phải là xanh");
    }

    @Test
    void transparentPngIsPlacedOnWhiteBackground() throws Exception {
        BufferedImage transparent = new BufferedImage(300, 400, BufferedImage.TYPE_INT_ARGB); // toàn bộ trong suốt
        BufferedImage out = decode(AnhHoSoUtil.chuanHoa(write("t.png", transparent, "png")));
        Color pixel = new Color(out.getRGB(150, 200));
        assertTrue(pixel.getRed() > 240 && pixel.getGreen() > 240 && pixel.getBlue() > 240);
    }

    @Test
    void rejectsFilesThatAreNotImages() throws Exception {
        File text = temporary.resolve("x.jpg").toFile();
        Files.writeString(text.toPath(), "không phải ảnh");
        IOException error = assertThrows(IOException.class, () -> AnhHoSoUtil.chuanHoa(text));
        assertTrue(error.getMessage().contains("Không đọc được ảnh"));
    }
}
