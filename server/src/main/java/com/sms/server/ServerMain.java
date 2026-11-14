package com.sms.server;

import com.sms.server.config.AppConfig;
import com.sms.server.jpa.JpaUtil;
import com.sms.server.seed.DataSeeder;
import com.sms.server.service.AnhHoSoDiTru;
import com.sms.server.service.AnhHoSoKho;
import com.sms.server.rmi.RmiServer;
import com.sms.server.security.SessionManager;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) throws java.rmi.RemoteException {
        AppConfig config = AppConfig.load();
        System.setProperty("java.rmi.server.hostname", config.hostname());
        CountDownLatch shutdown = new CountDownLatch(1);
        try (JpaUtil jpa = new JpaUtil(config); SessionManager sessions = new SessionManager()) {
            DataSeeder seeder = new DataSeeder(jpa, config.hocKyDangKy());
            seeder.seedIfEmpty();
            seeder.seedChuongTrinhKhungIfEmpty();
            seeder.seedLichThiIfEmpty();
            // Chuyển ảnh hồ sơ cũ (lưu trong cơ sở dữ liệu) ra tệp; chỉ chạy khi khởi động server thật
            try {
                AnhHoSoDiTru.chay(jpa, new AnhHoSoKho(config.anhThuMuc()));
            } catch (java.io.IOException e) {
                throw new java.io.UncheckedIOException("Không chuẩn bị được thư mục ảnh hồ sơ", e);
            }
            try (RmiServer rmi = new RmiServer(config, jpa, sessions)) {
                awaitShutdown(shutdown, jpa, sessions, rmi);
            }
        }
    }

    private static void awaitShutdown(CountDownLatch shutdown, JpaUtil jpa,
            SessionManager sessions, RmiServer rmi) {
        Thread hook = new Thread(() -> {
            log.info("Đang dừng máy chủ quản lý sinh viên.");
            try {
                rmi.close();
                sessions.close();
                jpa.close();
            } finally {
                shutdown.countDown();
            }
        }, "sms-shutdown");
        Runtime.getRuntime().addShutdownHook(hook);

        log.info("Máy chủ đã khởi động. Nhấn Ctrl+C để dừng.");
        try {
            shutdown.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Luồng máy chủ đã nhận tín hiệu ngắt.");
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (IllegalStateException e) {
                log.debug("JVM đang thực hiện shutdown hook.", e);
            }
        }
    }
}
