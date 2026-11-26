# Console Store

Console Store là đồ án môn Công nghệ Java về quản lý cửa hàng máy console, tay cầm và đĩa game. Hệ thống gồm ứng dụng web dành cho khách hàng và nhân viên, cơ sở dữ liệu SQL Server, cùng chương trình Java độc lập để tra cứu tồn kho và bảo hành qua TCP Socket.

## Chức năng chính

| Nhóm chức năng | Nội dung |
| --- | --- |
| Cửa hàng | Xem, lọc và tra cứu sản phẩm, xem chi tiết và đánh giá, đăng ký, đăng nhập, quản lý tài khoản, giỏ hàng, đặt hàng, theo dõi đơn hàng |
| Bán hàng | Bán tại quầy, xử lý đơn online, duyệt thanh toán, tra cứu hóa đơn |
| Kho và hậu mãi | Nhập hàng, theo dõi tồn kho và biến động, thu mua thiết bị cũ, tiếp nhận và xử lý bảo hành |
| Quản lý | Quản lý sản phẩm, khách hàng, nhà cung cấp, nhân viên và tài khoản, xem báo cáo doanh thu và kho |
| Tác vụ nền và mạng | Quét đơn online quá hạn (CN23), tra cứu tồn kho và bảo hành qua Socket (CN24), chat hỗ trợ trên web |

## Công nghệ và yêu cầu

- JDK 17. Kiểm tra bằng `java -version`.
- Spring Boot 4.1.2, Spring MVC, Spring Security, Spring Data JPA, Thymeleaf và Bootstrap 5.
- Microsoft SQL Server và SSMS. Bật TCP/IP nếu instance chưa nhận kết nối qua TCP.
- Maven Wrapper có sẵn. Lần đầu chạy cần kết nối Internet để tải Maven và các thư viện.

## Cấu trúc dự án

| Đường dẫn | Nội dung |
| --- | --- |
| [`App/ConsoleStore/`](App/ConsoleStore/) | Ứng dụng web Maven. Controller nhận yêu cầu, Service xử lý nghiệp vụ, Repository truy vấn SQL Server và Thymeleaf dựng giao diện. |
| [`application.properties`](App/ConsoleStore/src/main/resources/application.properties) | Cấu hình SQL Server, thanh toán, Socket và tác vụ nền. |
| [`Data/Store.sql`](Data/Store.sql) | Script SQL Server tạo cơ sở dữ liệu, bảng, thủ tục, trigger và dữ liệu mẫu. |
| [`Client/`](Client/) | Client Socket độc lập, gồm [bản Swing](Client/LookupClientGui.java) và [bản dòng lệnh](Client/LookupClient.java). |
| [`BaoCao/`](BaoCao/) | Báo cáo đồ án. |

## 1. Tải mã nguồn và tạo cơ sở dữ liệu

Mở PowerShell:

```powershell
git clone https://github.com/Penguin-IT/Web-ConsoleStore.git
cd Web-ConsoleStore
```

1. Mở SSMS, kết nối đến instance SQL Server dùng để demo bằng tài khoản có quyền tạo cơ sở dữ liệu.
2. Mở [`Data/Store.sql`](Data/Store.sql) trong SSMS và chạy toàn bộ script. Script tạo cơ sở dữ liệu `QLStoreConsole` nếu chưa có, sau đó tạo cấu trúc và nạp dữ liệu mẫu.
3. Làm mới danh sách **Databases**, kiểm tra các bảng `SanPham`, `TonKho`, `TaiKhoan` và `HoaDon`. Ứng dụng dùng `ddl-auto=validate` để kiểm tra cấu trúc bảng khi khởi động.

## 2. Chạy ứng dụng web

Từ thư mục gốc của repo, nhập các lệnh dưới đây trong cùng một cửa sổ PowerShell. Thay tài khoản, mật khẩu và cổng theo instance SQL Server trên máy. `1433` là cổng ví dụ.

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:sqlserver://localhost:1433;databaseName=QLStoreConsole;encrypt=true;trustServerCertificate=true'
$env:SPRING_DATASOURCE_USERNAME = 'ten_dang_nhap_sql_server'
$env:SPRING_DATASOURCE_PASSWORD = 'mat_khau_sql_server'

cd App/ConsoleStore
.\mvnw.cmd spring-boot:run
```

Sau khi Spring Boot khởi động, mở [Console Store tại localhost](http://localhost:8080/). Giữ cửa sổ server chạy trong khi demo web hoặc client Socket. Thông tin kết nối SQL Server chỉ dùng trong cấu hình chạy cục bộ.

**Chạy bằng IntelliJ IDEA.** Mở `App/ConsoleStore/pom.xml` dưới dạng dự án Maven, chọn JDK 17, thêm ba biến kết nối SQL Server ở trên vào **Run → Edit Configurations → Environment variables**, rồi chạy lớp `ConsoleStoreApplication`.

### Tài khoản demo

Các tài khoản dưới đây dùng mật khẩu mẫu `123456` khi cài từ dữ liệu mẫu.

| Tài khoản | Vai trò | Dùng để demo |
| --- | --- | --- |
| `admin` | `ADMIN` | Quản lý và báo cáo, tra tồn kho và bảo hành |
| `nv02`, `nv03` | `NHANVIEN` | Thao tác nghiệp vụ theo quyền, tra tồn kho và bảo hành |
| `kh01`, `kh02` | `KHACHHANG` | Đặt hàng, xem đơn và kiểm thử hai khách đặt hàng đồng thời |

### Thanh toán demo

- **Online:** Có COD, chuyển khoản, MoMo và ZaloPay. COD là thanh toán khi nhận hàng. MoMo và ZaloPay dùng cổng mô phỏng PayLab. Chuyển khoản đi qua VNPAY Sandbox khi cấu hình VNPAY được bật, hoặc qua PayLab khi chưa bật.
- **Tại quầy:** Nhân viên lập hóa đơn và có thể nhận tiền mặt.
- Để demo PayLab, có thể chọn MoMo hoặc ZaloPay và làm theo hướng dẫn trên trang thanh toán. Để demo VNPAY, dùng thông tin Sandbox của nhóm trong cấu hình cục bộ.

## 3. Chạy client Socket (CN24)

Server tra cứu chạy cùng Spring Boot, mặc định lắng nghe TCP tại `127.0.0.1:9000`. Mở một PowerShell khác ở thư mục gốc của repo.

**Bản Swing**

```powershell
cd Client
java "-Dfile.encoding=UTF-8" LookupClientGui.java
```

Đăng nhập bằng tài khoản có quyền tra cứu. Chọn **Tra tồn kho** rồi nhập `SP1`, `SP2` hoặc `PlayStation`. Để kiểm tra bảo hành, chọn **Tra bảo hành** và nhập `SN-PS5STD-0001`. Tra cứu tồn kho trả về tổng số lượng trên các kho. Kết quả bảo hành phụ thuộc lịch sử bán và trạng thái của serial.

**Bản dòng lệnh**

```powershell
cd Client
chcp 65001
java "-Dfile.encoding=UTF-8" LookupClient.java 127.0.0.1 9000
```

Sau khi đăng nhập, thử các lệnh:

```text
TONKHO SP1
TONKHO PlayStation
BAOHANH SN-PS5STD-0001
QUIT
```

`ADMIN` và `NHANVIEN` dùng được cả hai chức năng tra cứu. Trong mã phân quyền, `KHO` được tra tồn kho và `BAOHANH` được tra bảo hành. Cổng `9100` trong cấu hình dành cho chat trên web.

## 4. Kịch bản demo và kiểm thử

| Kịch bản | Thao tác | Kết quả mong đợi |
| --- | --- | --- |
| Ba client Socket hoạt động đồng thời | Mở ba tab PowerShell và chạy client trong từng tab. Đăng nhập bằng `admin`, `nv02` và `nv03`. Giữ cả ba phiên mở, rồi gửi ba yêu cầu khác nhau gần như cùng lúc. | Mỗi client nhận đúng dữ liệu của lệnh đã gửi. Cửa sổ Run của server hiển thị các kết nối được xử lý trên các luồng `lookup-worker-*`. |
| Đóng một client | Gõ `QUIT` hoặc bấm **Thoát phiên** ở một client, rồi tra cứu tiếp từ hai client kia. | Phiên đã thoát đóng kết nối. Hai client còn lại tiếp tục nhận kết quả. |
| Hai khách đặt sản phẩm cuối cùng | Chọn sản phẩm có tổng tồn khả dụng bằng 1. Mở hai phiên trình duyệt riêng, đăng nhập `kh01` và `kh02` rồi đặt cùng sản phẩm gần như cùng lúc. Kiểm tra trước khi tác vụ hủy đơn chạy. | Chỉ một đơn được tạo và tồn khả dụng về 0. Yêu cầu còn lại nhận thông báo tồn kho và không làm tồn bị âm. |
| Tự động hủy đơn trả trước quá hạn | Đặt đơn online trả trước, để đơn ở trạng thái chờ xác nhận và chưa thanh toán. Theo dõi sau thời hạn mặc định 15 phút. | Lượt quét của `order-expiry-*` ghi nhận đơn bị hủy. Kiểm tra trạng thái đơn và tồn kho được hoàn lại. |

CN23 dùng thời hạn riêng cho COD đang chờ xác nhận (`store.order-expiry.cod-timeout-minutes`, mặc định 1440 phút). Khoảng nghỉ giữa hai lượt quét là 10 giây theo cấu hình hiện tại.

## Lỗi thường gặp

| Hiện tượng | Cách kiểm tra |
| --- | --- |
| Spring Boot báo lỗi datasource hoặc thiếu bảng | Kiểm tra dịch vụ SQL Server, cổng TCP, tài khoản, tên `QLStoreConsole` và kết quả chạy `Store.sql`. Nếu mở bằng IntelliJ, kiểm tra các biến môi trường trong cấu hình chạy. |
| `java` không được nhận diện | Kiểm tra JDK 17 và `PATH`. Mở lại Terminal sau khi cập nhật cấu hình. |
| Client báo không kết nối được | Giữ Spring Boot chạy, kiểm tra log `Store socket server started` và `store.lookup.enabled=true`. Bản Swing dùng cổng 9000. Nếu server đổi cổng, truyền cổng tương ứng khi chạy bản dòng lệnh. |
| Tìm `SP02` không ra sản phẩm | Dùng `SP2`, `2` hoặc tìm theo tên sản phẩm. |
| Client hiện tiếng Việt sai | Giữ file Java ở UTF-8, chạy với `"-Dfile.encoding=UTF-8"` và chọn mã trang 65001 cho Terminal dòng lệnh trên Windows. |
