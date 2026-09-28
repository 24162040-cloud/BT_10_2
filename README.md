# Project 2 — Thay JJWT bằng Nimbus JOSE + JWT

Chuyển ví dụ JJWT trong `04_JWT.pdf` (trang 15–34) sang Nimbus JOSE + JWT 10.10, dùng Spring Boot 3.5.16, Spring Security 6 và Java 21. Project này chạy độc lập, không cần project 1.

**Nimbus thay thư viện JJWT (`io.jsonwebtoken`), không thay chuẩn JWT.** Dự án vẫn phát hành JWT gồm `header.payload.signature`, ký HMAC SHA-256 (HS256).

## Chạy bài tập

Yêu cầu: JDK 21 và Maven 3.6.3 trở lên. Trong PowerShell tại thư mục `project-2-nimbus`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
mvn package
& "$env:JAVA_HOME\bin\java.exe" -jar target/jwt-nimbus-1.0.0.jar
```

Mở **http://localhost:8006/login**, tạo tài khoản rồi đăng nhập. Trang hồ sơ gọi `/users/me`; nút “Xem danh sách người dùng” gọi `/users`. “Đăng xuất” xóa token của tab và quay lại trang đăng nhập.

Mặc định dùng H2, lưu tài khoản trong `data/` qua các lần khởi động. Không cần cài cơ sở dữ liệu để chạy thử. Không đặt `JWT_SECRET` thì ứng dụng sinh khóa ngẫu nhiên mới mỗi lần chạy: token cũ sẽ mất hiệu lực khi khởi động lại.

Để giữ cùng khóa qua các lần chạy, tạo khóa một lần rồi lưu giá trị vào biến môi trường `JWT_SECRET` (Base64 của ít nhất 32 byte ngẫu nhiên):

```powershell
$jwtKey = New-Object byte[] 32
$jwtRng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtKey)
$jwtRng.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKey)
```

Lệnh trên đặt biến cho phiên PowerShell hiện tại. Không đưa khóa thật vào mã nguồn.

### Dùng MySQL như bài giảng

Tạo database trước:

```sql
CREATE DATABASE jwt_nimbus CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<mat-khau-MySQL-cua-ban>'
& "$env:JAVA_HOME\bin\java.exe" -jar target/jwt-nimbus-1.0.0.jar --spring.profiles.active=mysql
```

`DB_URL` là tùy chọn, mặc định `jdbc:mysql://localhost:3306/jwt_nimbus?serverTimezone=UTC`. Hibernate tự tạo bảng `users`. Database riêng để chạy cùng project 1. Profile MySQL cần máy chủ MySQL của bạn; kiểm tra tự động bên dưới sử dụng H2.

## Đối chiếu bài giảng

| Bước / trang | Mã nguồn thực hiện |
| --- | --- |
| Dependency, trang 15 | `pom.xml`: dùng `com.nimbusds:nimbus-jose-jwt`, không dùng JJWT |
| Entity, trang 16 | `entity/User.java`: ID, họ tên, email, ảnh, mật khẩu BCrypt, thời điểm tạo/cập nhật; implements `UserDetails` |
| Models, trang 17 | `models/RegisterUserModel`, `LoginUserModel`, `LoginResponse` |
| Repository và services, trang 18–21 | `UserRepository`, `AuthenticationService`, `JwtService`; danh sách dùng trực tiếp `findAll()` |
| Cấu hình và filter, trang 22–25 | `SecurityConfiguration`, `JwtAuthenticationFilter`; stateless, xác thực Bearer |
| REST API, trang 26–27 | `AuthenticationController`, `UserController` |
| Thử API, trang 28–29 | Ví dụ PowerShell bên dưới và `scripts/smoke_test.py` |
| Xử lý lỗi, trang 30–31 | `GlobalExceptionHandler`, authentication entry point và access denied handler |
| AJAX, trang 32–34 | `login.html`, `profile.html`, `js/mainjs.js`, `AuthController`; Fetch API có sẵn trong trình duyệt |

Các tệp Java nằm trong `src/main/java/vn/iotstar/`; cấu hình và giao diện trong `src/main/resources/`.

## Thử API bằng PowerShell / Postman

| Method | URL | Yêu cầu | Kết quả |
| --- | --- | --- | --- |
| POST | `/auth/signup` | JSON: `email`, `password`, `fullName` | User, không chứa mật khẩu |
| POST | `/auth/login` | JSON: `email`, `password` | `token`, `expiresIn` (mili giây) |
| GET | `/users/me` | `Authorization: Bearer <token>` | User hiện tại |
| GET | `/users` hoặc `/users/` | `Authorization: Bearer <token>` | Danh sách user |

```powershell
$base = 'http://localhost:8006'
$signup = @{email='student@example.com'; password='123456'; fullName='Nguyen Van An'} | ConvertTo-Json
Invoke-RestMethod "$base/auth/signup" -Method Post -ContentType 'application/json' -Body $signup

$credentials = @{email='student@example.com'; password='123456'} | ConvertTo-Json
$login = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType 'application/json' -Body $credentials
$headers = @{Authorization="Bearer $($login.token)"}
Invoke-RestMethod "$base/users/me" -Headers $headers
Invoke-RestMethod "$base/users" -Headers $headers
```

Trong Postman: chọn Body → raw → JSON cho hai POST; sau đăng nhập, sao chép `token` vào Authorization → Bearer Token khi gọi GET.

Mã lỗi: **400** dữ liệu sai; **401** sai thông tin đăng nhập, thiếu token, chữ ký sai hoặc hết hạn; **403** không đủ quyền / trạng thái tài khoản không hợp lệ; **409** email đã tồn tại. Ví dụ này chưa có chức năng khóa tài khoản hay vai trò; mọi user đăng nhập được xem `/users` như bài giảng.

## Thay JJWT bằng Nimbus

| Trong slide (JJWT) | Trong bài làm (Nimbus) |
| --- | --- |
| `Jwts.builder()` | `JWTClaimsSet.Builder` và `SignedJWT` |
| `signWith(..., Jwts.SIG.HS256)` | `jwt.sign(new MACSigner(secret))` |
| `compact()` | `jwt.serialize()` |
| Parser JJWT | `SignedJWT.parse(token)` |
| Kiểm tra chữ ký | `jwt.verify(new MACVerifier(secret))` |
| `Claims.getSubject()` | `jwt.getJWTClaimsSet().getSubject()` sau khi xác minh |

Luồng: đăng ký → BCrypt lưu mật khẩu → `AuthenticationManager` xác thực email/mật khẩu → Nimbus tạo JWT → AJAX gửi Bearer → filter xác minh JWT và tải user → đặt `SecurityContext` → controller trả dữ liệu.

`JwtService.extractUsername()` kiểm tra HS256, chữ ký, `iss`, `aud`, `sub`, `exp`, `iat` và `nbf` nếu có. Không chỉ giải mã token rồi tin nội dung. Thời hạn mặc định là **1 giờ**, dùng cùng `security.jwt.expiration-time` cho cả claim `exp` và `expiresIn` (sửa chỗ slide đặt cứng 30 giờ).

Theo [tài liệu Nimbus về HMAC](https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac), HS256 cần khóa tối thiểu 256 bit và `verify()` chỉ kiểm tra chữ ký; ứng dụng phải kiểm tra claims riêng. JWT được ký trong bài này **không mã hóa payload**; không đưa mật khẩu vào token. [Tài liệu Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html).

AJAX dùng `sessionStorage` để giữ token trong tab, và `textContent` để hiển thị dữ liệu. Cách lưu này vẫn chịu rủi ro XSS. Đăng xuất chỉ xóa bản token phía trình duyệt, không thu hồi các bản sao còn hạn. Các lựa chọn này phục vụ ví dụ stateless trong bài giảng; khi triển khai thật cần HTTPS và thiết kế lưu/thu hồi token phù hợp.

## Kiểm tra tự động

Không cần cài thư viện Python. Script tự mở ứng dụng ở cổng tạm, dùng H2 trong bộ nhớ và khóa thử nghiệm, rồi dừng tiến trình sau kiểm tra; không tác động tài khoản trong `data/`.

```powershell
mvn package
python scripts/smoke_test.py
```

Kiểm tra các trang và tài nguyên, đăng ký/đăng nhập, validation, email trùng không phân biệt hoa thường, giới hạn mật khẩu 72 byte BCrypt, chữ ký HS256 độc lập bằng Python, thời hạn thực khớp `expiresIn`, bảo vệ `/users/me` và `/users`, không lộ mật khẩu và 14 trường hợp token lỗi. Nhật ký máy chủ thử nghiệm: `target/smoke-test.log`.

Kết quả kiểm tra: Maven package thành công, smoke test PASS, `node --check src/main/resources/static/js/mainjs.js` thành công. Chưa kiểm tra tương tác trong trình duyệt vì môi trường không có trình duyệt kết nối; chưa chạy profile MySQL.
