package com.sms.client.rmi;

import com.sms.common.rmi.RmiNames;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Cấu hình kết nối: ưu tiên file client.properties cạnh nơi chạy, sau đó bản đóng gói trong jar. */
public record ClientConfig(String host, int port, String hocKyMacDinh) {

    public static ClientConfig load() {
        Properties props = new Properties();
        Path local = Path.of("client.properties");
        try {
            if (Files.isRegularFile(local)) {
                try (Reader reader = Files.newBufferedReader(local, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
            } else {
                try (InputStream in = ClientConfig.class.getResourceAsStream("/client.properties")) {
                    if (in != null) {
                        props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được client.properties", e);
        }
        String host = props.getProperty("server.host", "localhost").trim();
        int port;
        try {
            port = Integer.parseInt(props.getProperty("server.port",
                    String.valueOf(RmiNames.DEFAULT_REGISTRY_PORT)).trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("server.port phải là số", e);
        }
        return new ClientConfig(host, port, props.getProperty("hocky.mac.dinh", "HK1-2026-2027").trim());
    }
}
