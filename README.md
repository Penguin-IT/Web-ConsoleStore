# Console Store

Đồ án môn Công nghệ Java. Console Store là ứng dụng quản lý cửa hàng máy console, tay cầm và đĩa game. Hệ thống hỗ trợ bán hàng online và tại quầy, nhập hàng, theo dõi tồn kho, thu mua thiết bị cũ, bảo hành và báo cáo. Nhân viên có thể dùng chương trình Java Socket riêng để tra cứu tồn kho và bảo hành.

## 1. Công nghệ và yêu cầu cài đặt

- **JDK 17**. Kiểm tra bằng `java -version`.
- Spring Boot, Spring Security, Spring Data JPA, Thymeleaf và Bootstrap.
- Microsoft SQL Server và SQL Server Management Studio (SSMS).
- Maven Wrapper có sẵn trong dự án. Lần chạy đầu cần Internet để tải Maven và các thư viện.

## 2. Cấu trúc dự án

| Đường dẫn | Nội dung |
|---|---|
| `App/ConsoleStore/` | Ứng dụng web Spring Boot |
| `App/ConsoleStore/src/main/resources/application.properties` | Cấu hình ứng dụng, SQL Server và cổng Socket |
| `Data/Store.sql` | Script tạo cơ sở dữ liệu và nạp dữ liệu mẫu |
| `Client/LookupClient.java` | Chương trình tra cứu chạy độc lập qua TCP Socket |

## 3. Tạo cơ sở dữ liệu

1. Mở SSMS và kết nối vào SQL Server bằng tài khoản có quyền tạo cơ sở dữ liệu.
2. Mở [`Data/Store.sql`](Data/Store.sql) và chạy toàn bộ script trên môi trường demo. Script tạo `QLStoreConsole` nếu chưa có, sau đó tạo cấu trúc và nạp dữ liệu mẫu.
3. Làm mới danh sách **Databases** trong SSMS. Kiểm tra `QLStoreConsole` có các bảng như `SanPham`, `TaiKhoan`, `TonKho` và `HoaDon`.

## 4. Chạy server web

Mở PowerShell và tải mã nguồn:

```powershell
git clone https://github.com/Penguin-IT/Web-ConsoleStore.git
cd Web-ConsoleStore
```

Trong PowerShell, thiết lập thông tin SQL Server của máy đang chạy:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://localhost:1433;databaseName=QLStoreConsole;encrypt=true;trustServerCertificate=true'
$env:SPRING_DATASOURCE_USERNAME = 'ten_dang_nhap_sql_server'
$env:SPRING_DATASOURCE_PASSWORD = 'mat_khau_sql_server'
```

`localhost:1433` là địa chỉ ví dụ. Thay máy chủ, cổng, tên đăng nhập và mật khẩu theo cấu hình SQL Server đang dùng. Đây là thông tin kết nối cơ sở dữ liệu, khác với tài khoản đăng nhập trên website. Giữ mật khẩu SQL Server trong cấu hình chạy cục bộ.

Trong **cùng cửa sổ PowerShell**, chạy:

```powershell
cd App/ConsoleStore
.\mvnw.cmd spring-boot:run
```

Khi ứng dụng khởi động xong, mở [http://localhost:8080](http://localhost:8080). Giữ server hoạt động trong khi chạy client Socket.

Nếu chạy bằng IntelliJ IDEA, mở `App/ConsoleStore/pom.xml` dưới dạng dự án Maven, chọn JDK 17, thêm ba biến kết nối SQL Server vào cấu hình chạy và chạy lớp `ConsoleStoreApplication`.

## 5. Tài khoản và chức năng demo

Các tài khoản dưới đây thuộc dữ liệu mẫu trong `Store.sql`. Mật khẩu mẫu là `123456`.

| Tài khoản | Vai trò | Chức năng có thể demo |
|---|---|---|
| `admin` | Quản trị viên | Quản lý sản phẩm, nhân viên, tài khoản và xem báo cáo |
| `nv02`, `nv03` | Nhân viên | Bán tại quầy, xử lý đơn online, nhập hàng, tra tồn kho và bảo hành |
| `kh01` | Khách hàng | Xem sản phẩm, thêm vào giỏ, đặt hàng và xem đơn hàng |

## 6. Chạy client Socket

Mở một PowerShell khác tại thư mục gốc của repository, sau đó chạy:

```powershell
cd Client
java LookupClient.java 127.0.0.1 9000
```

Đăng nhập bằng tài khoản `admin` hoặc tài khoản nhân viên có quyền. Các lệnh mẫu:

```text
TONKHO SP1
TONKHO PlayStation
BAOHANH SN-PS5STD-0001
QUIT
```

`ADMIN` và `NHANVIEN` dùng được cả hai lệnh tra cứu. Tài khoản `KHO` chỉ dùng được `TONKHO`, còn `BAOHANH` chỉ dùng được lệnh `BAOHANH`. Client hiển thị các lệnh được phép sau khi đăng nhập.

## 7. Demo xử lý đồng thời và lập trình mạng

1. **Ba client cùng kết nối.** Mở ba cửa sổ Terminal, chạy lệnh ở mục 6 trong từng cửa sổ, rồi đăng nhập lần lượt bằng `admin`, `nv02` và `nv03`. Giữ cả ba phiên mở và gửi lệnh tra cứu gần như cùng lúc. Kiểm tra từng client nhận đúng kết quả. Sau đó gửi `QUIT` ở một client và kiểm tra hai client còn lại vẫn tra cứu được.
2. **Hai yêu cầu cùng đặt sản phẩm cuối cùng.** Chuẩn bị sản phẩm có tổng tồn khả dụng bằng 1. Đăng nhập hai tài khoản khách hàng trong hai phiên trình duyệt riêng rồi đặt cùng sản phẩm. Kết quả mong đợi là chỉ một đơn được tạo và tồn kho cuối cùng bằng 0.
3. **Tự động hủy đơn quá hạn.** Theo dõi một đơn online thanh toán trước đang chờ xác nhận và chưa thanh toán. Cấu hình hiện tại đặt thời hạn 15 phút và khoảng nghỉ giữa hai lượt quét là 10 giây. Kiểm tra log `order-expiry-`, trạng thái đơn và số lượng được hoàn vào kho. Đơn COD thuộc luồng thanh toán khi nhận hàng và được loại khỏi lượt quét.

## 8. Xử lý lỗi thường gặp

- **Web báo lỗi kết nối SQL Server:** Kiểm tra dịch vụ SQL Server, máy chủ, cổng, thông tin đăng nhập và quyền truy cập `QLStoreConsole`.
- **Web báo thiếu bảng hoặc cột:** Kiểm tra đã chạy hết `Data/Store.sql` và ứng dụng đang kết nối đúng cơ sở dữ liệu.
- **Client không kết nối được:** Kiểm tra server đã khởi động, `store.lookup.enabled=true` và cổng client trùng với `store.lookup.port`.
- **Client đăng nhập được nhưng bị từ chối lệnh:** Kiểm tra vai trò của tài khoản và danh sách lệnh server hiển thị sau khi đăng nhập.
