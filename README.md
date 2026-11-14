# Quản lý sinh viên

## Cấu trúc

- `common`: hợp đồng RMI, DTO, enum và exception; không phụ thuộc JPA/JavaFX.
- `server`: Hibernate 6.6.4.Final, JPA 3.1, HikariCP qua `hibernate-hikaricp`,
  MariaDB JDBC 3.4.1 và jBCrypt 0.4.
- `client`: JavaFX Controls/FXML 21.0.5, AtlantaFX 2.0.1 (PrimerLight).
- Logging: SLF4J 2.0.16, Logback 1.5.12. Lombok 1.18.34 (`provided`)
- RMI có sẵn trong JDK 17, không cần thêm dependency. Không dùng JPMS.

## Build và chạy

Cài JDK 17 và chọn JDK này riêng cho dự án trong IDE hoặc phiên terminal
(không cần đổi `JAVA_HOME` toàn máy). Maven Wrapper tải Maven ở lần chạy đầu,
không cần cài Maven riêng. Lần đầu cần mạng để tải Maven và thư viện.

Nếu `java -version` và `.\mvnw.cmd -version` dùng JDK khác nhau, kiểm tra lại
`JAVA_HOME`; Maven ưu tiên biến này thay vì Java trong `PATH`.

Chạy tại thư mục gốc bằng PowerShell:

```powershell
.\mvnw.cmd clean install
.\run-server.cmd
```

Mở terminal thứ hai để chạy client:

```powershell
.\run-client.cmd
```
