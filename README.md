# Hướng Dẫn Cài Đặt và Chạy Dự Án (Project Setup Guide)

Dự án này là một ứng dụng Spring Boot Web bán hàng. Để chạy được dự án, bạn cần cài đặt các phần mềm sau:

## 1. Yêu Cầu Hệ Thống (Prerequisites)

### Java Development Kit (JDK)
- **Phiên bản:** JDK 17 trở lên.
- **Kiểm tra:** Mở terminal (CMD/PowerShell) và gõ `java -version`.
- **Cài đặt:** Nếu chưa có, hãy tải và cài đặt từ [Oracle](https://www.oracle.com/java/technologies/downloads/#java17) hoặc OpenJDK.
- **Lưu ý:** Đảm bảo biến môi trường `JAVA_HOME` đã được thiết lập trỏ đến thư mục cài đặt JDK 17.

### Cơ Sở Dữ Liệu MySQL
- **Phiên bản:** MySQL 8.0 trở lên.
- **Cài đặt:** Tải và cài đặt MySQL Server từ [MySQL Installer](https://dev.mysql.com/downloads/installer/).
- **Cấu hình:** Đảm bảo MySQL đang chạy ở cổng mặc định `3306`.

## 2. Cấu Hình Cơ Sở Dữ Liệu

Dự án được cấu hình để tự động kết nối và tạo bảng dữ liệu. Tuy nhiên, bạn cần tạo trước database rỗng và cấu hình tài khoản:

1.  **Tạo Database:**
    Mở MySQL Workbench hoặc Command Line và chạy lệnh sau:
    ```sql
    CREATE DATABASE quan_ly_ban_hang;
    ```

2.  **Cấu hình Tài Khoản:**
    Mặc định dự án sử dụng tài khoản `root` với mật khẩu `root`. Nếu cấu hình của bạn khác, hãy mở file `src/main/resources/application.properties` và sửa lại:
    ```properties
    spring.datasource.username=tên_đăng_nhập_của_bạn
    spring.datasource.password=mật_khẩu_của_bạn
    ```

## 3. Cách Chạy Dự Án

### Sử dụng Maven Wrapper (Khuyên dùng)
Dự án đã tích hợp sẵn Maven Wrapper, bạn không cần cài đặt Maven riêng.

1.  Mở terminal tại thư mục gốc của dự án (`d:\DoAn2`).
2.  Chạy lệnh:
    ```powershell
    .\mvnw.cmd spring-boot:run
    ```
    (Trên Linux/Mac dùng `./mvnw spring-boot:run`)

### Truy cập Ứng Dụng
Sau khi chạy thành công, mở trình duyệt và truy cập:
- **Trang chủ:** [http://localhost:8080](http://localhost:8080)
- **Trang Admin (nếu có):** [http://localhost:8080/admin](http://localhost:8080/admin) (hoặc đường dẫn tương ứng)

## 4. Troubleshooting (Sửa Lỗi Thường Gặp)

- **Lỗi "Java version":** Kiểm tra lại biến môi trường `JAVA_HOME`.
- **Lỗi "Connection refused":** Kiểm tra MySQL service đã chạy chưa.
- **Lỗi đăng nhập Database:** Kiểm tra lại username/password trong `application.properties`.
