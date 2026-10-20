# Hệ thống quản lý đề tài sinh viên — Khoa CNTT

Ứng dụng quản lý toàn bộ quy trình đề tài (môn học / NCKH / TLCN / KLTN) của khoa CNTT: tạo đợt đăng ký, giảng viên đề xuất đề tài, sinh viên lập nhóm và đăng ký, hội đồng phản biện, chấm điểm, tổng hợp và công bố kết quả.

> **Giai đoạn hiện tại: GĐ12 — Rà soát toàn diện, Postman collection, README hoàn thiện.** 13/13 giai đoạn đã hoàn thành với 118/118 unit + integration tests xanh. Xem `docs/ke-hoach-trien-khai.md` cho toàn bộ kế hoạch 13 giai đoạn.

---

## 1. Yêu cầu môi trường

| Phần mềm | Phiên bản |
|----------|-----------|
| JDK      | 21 (LTS) |
| Maven    | 3.9.x (đã có Maven Wrapper) |
| MySQL    | 8.0+ |
| Git      | bất kỳ |

Công cụ kiểm thử giao diện: **Google Chrome** + DevTools.

---

## 2. Cài đặt & chạy lần đầu

### 2.1 Lấy mã nguồn
```bash
git clone <repo-url>
cd "Student Project Registration Page"
```

### 2.2 Khởi tạo Maven Wrapper (chỉ làm 1 lần sau khi clone)
Trong repo có sẵn `mvnw` và `mvnw.cmd`. Để sinh `maven-wrapper.jar` tương ứng (chưa commit lên để tránh binary), chạy:
```bash
mvn -N wrapper:wrapper
```
Sau bước này `./mvnw` sẽ hoạt động.

### 2.3 Tạo cơ sở dữ liệu
Mở **MySQL Workbench**, kết nối đến MySQL, mở file `docs/sql/HT_QLDT.sql` rồi bấm **Run SQL Script** (Ctrl+Shift+Enter). Script này sẽ:
- Tạo mới database `hethong_quanlydetai` (drop nếu đã tồn tại)
- Tạo 24 bảng với ràng buộc đặt tên rõ
- Chèn sẵn 5 vai trò hệ thống, 19 quyền và ánh xạ quyền-vai trò mặc định

> **Lưu ý:** Script sẽ xóa database `hethong_quanlydetai` nếu đã tồn tại. Chỉ chạy khi khởi tạo database mới hoặc sau khi sao lưu dữ liệu cần giữ.

Để vẽ sơ đồ EER: trong Workbench, **Database → Reverse Engineer…** → chọn schema `hethong_quanlydetai`.

### 2.4 Tạo dữ liệu mẫu (chỉ chạy khi cần)
Mở file `docs/seed/HT_QLDT_demo.sql` trong Workbench và chạy. Script chèn:
- 1 admin, 1 trưởng khoa, 2 chủ tịch hội đồng, 6 giảng viên, 12 sinh viên
- 3 bộ môn, 2 đợt đăng ký (TLCN & KLTN)
- Một số đề tài ở các trạng thái, 1 nhóm SV mẫu, 2 thông báo mẫu

Hash BCrypt trong script có tiền tố `{bcrypt}` theo yêu cầu của Spring Security `DelegatingPasswordEncoder`.

### 2.5 Cấu hình kết nối (tùy chọn)
Mặc định đọc từ biến môi trường; nếu không có thì dùng local dev (`root`/`root` trên `localhost:3306`).

| Biến | Mặc định |
|------|----------|
| `DB_URL`      | `jdbc:mysql://localhost:3306/hethong_quanlydetai?...` |
| `DB_USERNAME` | `root` |
| `DB_PASSWORD` | `root` |
| `SERVER_PORT` | `8080` |
| `APP_STORAGE_DIR` | `~/.hethong-quanlydetai/reports` |
| `APP_LOGIN_MAX_FAIL` | `5` |
| `APP_LOGIN_LOCK_MINUTES` | `15` |

### 2.6 Build & chạy
```bash
./mvnw test           # chạy test
./mvnw spring-boot:run
```
Mở http://localhost:8080, đăng nhập bằng tài khoản demo (xem mục 3).

### 2.7 Chạy với dữ liệu demo
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```
(Chỉ cần thiết nếu bạn chưa nạp `docs/seed/HT_QLDT_demo.sql` thủ công; với profile=demo, ứng dụng sẽ cố gắng nạp file SQL ở `classpath:db/seed/HT_QLDT_demo.sql`. Ở GĐ1, dữ liệu mẫu được nạp bằng tay qua Workbench để giữ script chuẩn tách khỏi mã.)

---

## 3. Tài khoản demo

> **Mật khẩu chung: `Demo@123`** — chỉ dùng cho môi trường DEMO. KHÔNG dùng cho production.

| Username | Vai trò |
|----------|---------|
| `admin`        | Quản trị hệ thống |
| `truongkhoa`   | Trưởng khoa |
| `cthd1`, `cthd2` | Chủ tịch hội đồng |
| `gv_cnpm1`, `gv_cnpm2`, `gv_khmt1`, `gv_khmt2`, `gv_mmt1`, `gv_mmt2` | Giảng viên |
| `sv01` … `sv12` | Sinh viên |

---

## 4. Cấu trúc thư mục

```
.
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
├── src/
│   ├── main/
│   │   ├── java/blog/hethong_quanlydetai/
│   │   │   ├── HethongQuanlydetaiApplication.java
│   │   │   ├── audit/         # AuditLogger
│   │   │   ├── config/        # SecurityConfig, WebConfig, DataBootstrapConfig
│   │   │   ├── controller/    # MVC controllers (Login, Dashboard, Error, admin/*, me/*)
│   │   │   ├── domain/        # Entity + enums (User, Role, Topic, ...)
│   │   │   ├── dto/           # UserForm, RoleForm, PasswordChangeForm...
│   │   │   ├── exception/     # BusinessException, GlobalExceptionHandler
│   │   │   ├── repository/    # Spring Data JPA repositories
│   │   │   ├── security/      # UserDetailsService, LoginAttemptService, AppUserPrincipal
│   │   │   └── service/       # UserService, RoleService, PermissionService, PasswordService...
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-demo.properties
│   │       ├── static/css/{theme,layout,components}.css
│   │       ├── static/js/app.js
│   │       └── templates/{login.html, dashboard.html, fragments/layout.html, error/*.html, admin/*, me/*}
│   └── test/java/blog/hethong_quanlydetai/{StaticBootstrapTest, UserManagementTest, SecurityAccessTest}.java
├── docs/
│   ├── sql/HT_QLDT.sql          # schema chuẩn (chạy sạch trên MySQL trống)
│   ├── seed/HT_QLDT_demo.sql    # dữ liệu mẫu (chạy sau schema)
│   └── ke-hoach-trien-khai.md   # báo cáo & kế hoạch 13 giai đoạn
└── README.md
```

---

## 5. Công nghệ & phiên bản

- Spring Boot 3.5.x (Jakarta EE 10), Java 21
- Spring Security (form login, CSRF, BCrypt `DelegatingPasswordEncoder`)
- Spring Data JPA / Hibernate (`ddl-auto=validate`, `open-in-view=false`)
- MySQL Connector/J, múi giờ `Asia/Ho_Chi_Minh`
- Thymeleaf + thymeleaf-extras-springsecurity6
- Maven Wrapper (Maven 3.9.9), JUnit 5, Spring Boot Test

Không thêm thư viện trùng chức năng: chỉ JS thuần, CSS thuần (3 tệp hợp nhất).

---

## 6. Báo cáo Giai đoạn 0 (xác minh công nghệ)

> Workspace `d:\Student Project Registration Page` ban đầu **trống** — không có `pom.xml`, không có mã nguồn cũ. Tôi đã xin phép bootstrap từ đầu và được duyệt. Bộ công nghệ mục 1 của đề bài được giữ nguyên.

| # | Mục | Dự kiến | Thực tế | Kết luận |
|---|-----|--------|---------|----------|
| 1 | `pom.xml` parent | Spring Boot 4.1.1 (chưa phát hành ổn định) | Tạo mới với 3.5.x | Phù hợp JDK 21, ổn định |
| 2 | `java.version` | 21 | 21 | Khớp |
| 3 | `application.properties` | MySQL, `ddl-auto=validate`, `open-in-view=false`, env vars | Tạo mới | Khớp |
| 4 | `src/main/resources` | `templates/`, `static/css/`, `static/js/` | Tạo mới | Khớp |
| 5 | `src/test` | `./mvnw test` chạy được | 1 ca thành công | Khớp |
| 6 | IDE / Workbench / Postman | `.vscode`, `*.mwb`, `*.postman_collection.json` | Bỏ qua `.vscode` (hệ thống không cho tạo) | Hướng dẫn trong README |

---

## 7. Báo cáo Giai đoạn 1 (nền tảng)

### Các tệp chính đã tạo
- `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`
- `application.properties`, `application-demo.properties`
- `docs/sql/HT_QLDT.sql` — 18 bảng + 5 vai trò + 19 quyền + 5 ánh xạ vai trò-quyền
- `docs/seed/HT_QLDT_demo.sql`
- Toàn bộ package `blog.hethong_quanlydetai.{domain, repository, service, security, audit, config, controller, exception}` (entity, repo, service, security, audit, exception)
- Templates: `login.html`, `dashboard.html`, `fragments/layout.html`, `error/{403,404,500}.html`
- CSS: `theme.css`, `layout.css`, `components.css`
- JS: `app.js`
- Test: `StaticBootstrapTest`

### Kết quả build
- `./mvnw test`: tối thiểu 1 ca PASS (`StaticBootstrapTest.packageConstantCompiles` + `periodTypeRequiresFieldsAreCorrect`).
- Đăng nhập thành công với `admin` / `Demo@123` (sau khi nạp demo SQL).
- Dashboard hiển thị theo vai trò; menu chỉ chứa mục người dùng có quyền.

---

## 7.1 Báo cáo Giai đoạn 2 (tài khoản, vai trò, phân quyền, đổi mật khẩu, nhật ký)

### Tệp chính đã thêm/sửa

**Domain & repository**
- `domain/BaseEntity.java` — tự set `created_at`/`updated_at` (cần cho H2 ở test).
- `domain/User`, `Role`, `Permission` chuẩn hoá mapping (`@Builder.Default`).
- `repository/UserRepository`, `RoleRepository`, `PermissionRepository` (tìm theo tên/email/role + phân trang).

**Service & security**
- `service/UserService` — tạo/sửa/khóa/mở khoá/reset mật khẩu; **chặn tự khoá chính mình**, chặn bỏ vai trò `ADMIN` cuối cùng; tự sinh hồ sơ `Student`/`Lecturer` khi cần.
- `service/RoleService` — tạo/sửa/xoá vai trò; **không xoá vai trò hệ thống**.
- `service/PermissionService` — liệt kê quyền, gán/bỏ quyền theo vai trò.
- `service/PasswordService` — đổi mật khẩu (kiểm tra mật khẩu cũ, độ phức tạp ≥ 8 ký tự có chữ + số).
- `service/AuditLogger` (`Propagation.REQUIRES_NEW`) ghi `audit_logs` cho mọi hành động nhạy cảm.
- `security/AppUserDetailsService`, `AppUserPrincipal`, `LoginAttemptService` (khoá 15 phút sau 5 lần sai).

**Controller (MVC)**
- `controller/admin/UserAdminController` (`@PreAuthorize("hasAuthority('USER_MANAGE')")`) — danh sách, tạo, sửa, đổi vai trò, khoá/mở, reset mật khẩu.
- `controller/admin/RoleAdminController` (`ROLE_MANAGE`).
- `controller/admin/PermissionAdminController` (`PERMISSION_MANAGE`).
- `controller/MeController` — hồ sơ cá nhân, đổi mật khẩu.

**Templates (Thymeleaf)**
- `templates/fragments/layout.html` — sidebar theo quyền, topbar, flash, page-header, pagination, badge, scripts.
- `templates/admin/users/{list,form}.html`
- `templates/admin/roles/{list,form}.html`
- `templates/admin/permissions/list.html`
- `templates/me/{profile,change-password}.html`

**Test (JUnit + Spring Boot Test)**
- `UserManagementTest` (12 case) — tạo/sửa user, role, đổi mật khẩu đúng/sai, khoá chính mình, bỏ admin cuối cùng, role hệ thống.
- `SecurityAccessTest` (7 case) — chưa đăng nhập → `/login`; `@WithMockUser(authorities=...)` kiểm tra phân quyền; CSRF bắt buộc trên POST.
- `StaticBootstrapTest` (2 case — bảo trì qua các giai đoạn).

### Kết quả `./mvnw test`

```
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class | Pass | Tổng |
|------------|:---:|:---:|
| SecurityAccessTest | 7 | 7 |
| StaticBootstrapTest | 2 | 2 |
| UserManagementTest | 12 | 12 |

---

## 7.2 Báo cáo Giai đoạn 3 (đợt đăng ký)

### Tệp chính đã thêm/sửa
- `repository/RegistrationPeriodRepository` — search/lọc, `findOpenForStudents/Lecturers`, `hasAnyTopic`.
- `service/RegistrationPeriodService` — validate quy tắc thời gian + loại, chuyển trạng thái, audit log.
- `dto/RegistrationPeriodForm` — Bean Validation cơ bản (tên, loại, các mốc thời gian).
- `controller/admin/RegistrationPeriodAdminController` (`@PreAuthorize("hasAuthority('PERIOD_MANAGE')")`) — list, new, edit, publish, close, delete.
- Templates: `admin/periods/{list,form,detail}.html` (dùng fragment layout, JS ẩn/hiện trường theo loại).
- `fragments/layout.html` — sidebar link `/admin/periods`.
- Test: `RegistrationPeriodTest` (13 case).

### Quy tắc enforce ở service
- `lecturerStartAt < lecturerEndAt`; `studentStartAt < studentEndAt`.
- `studentStartAt >= lecturerEndAt`.
- `reviewDeadline`: bắt buộc với `TLCN`/`KLTN`; phải sau `studentEndAt`.
- `councilReportDate`: chỉ ở `KLTN`; phải sau `reviewDeadline`.
- `PUBLISHED`: chỉ sửa mốc thời gian (không đổi loại/tên).
- Xoá: chỉ `DRAFT` và chưa có Topic.

### Kết quả `./mvnw test`

```
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class | Pass | Tổng |
|------------|:---:|:---:|
| RegistrationPeriodTest | 13 | 13 |
| SecurityAccessTest | 7 | 7 |
| StaticBootstrapTest | 2 | 2 |
| UserManagementTest | 12 | 12 |

### 7.6 GĐ5 — Nhóm SV & Đăng ký đề tài (đã hoàn thành)
- `service/StudentGroupService`: tạo nhóm DRAFT, thêm/xoá/đổi trưởng thành viên, enforce cùng lớp + 1–3 SV + unique group/period, khoá khi APPROVED.
- `service/RegistrationService`: đăng ký/đổi/huỷ, duyệt/từ chối với lý do ≥ 10 ký tự, chỉ GVHD chính `supervisor_order=1`, đề tài APPROVED+published, 1 topic ↔ 1 group.
- Controller: `/groups` (SV), `/registrations` (GVHD).
- `GroupRegistrationTest` (21 case). Tổng: **73/73 xanh**.

### 7.7 GĐ6 — Báo cáo (đã hoàn thành)
- `service/ReportService`: upload file, version tăng dần (`SUBMITTED` mới, các bản cũ `SUPERSEDED`), tối đa 20MB, đuôi .pdf/.doc/.docx, lưu `app.storage.dir`, chỉ nhóm trưởng nộp, nhóm phải `APPROVED`/`SUBMITTED`.
- Controller: `/reports/**`. Template: `reports/{index,list,upload}`.
- `ReportSubmissionTest` (6). Tổng: **79/79 xanh**.

### 7.8 GĐ7 — Hội đồng (đã hoàn thành)
- `service/CouncilService`: CRUD + thành viên 3..5 người (1 CHAIRMAN, 1 SECRETARY, 1–3 MEMBER, không trùng GV), gán 1 topic ↔ 1 HĐ tại 1 thời điểm, lifecycle `DRAFT → READY → FINISHED`, kiểm tra `council_report_date` đã qua.
- Controller: `/councils/**`. Template: `councils/{list,form,detail}`.
- `CouncilTest` (10). Tổng: **89/89 xanh**.

### 7.9 GĐ8 — Phân công & Nhập điểm (đã hoàn thành)
- `service/EvaluationService`: phân công GV chấm từng đề tài, nhập điểm 0–10 (HALF_UP 2 chữ số), comment ≥ 10 ký tự, 1 GV chỉ chấm 1 lần / 1 đề tài trong 1 HĐ.
- Controller: `/assignments/**` (CTHD), `/evaluations/**` (GV được phân công).
- Template: `assignments/list`, `evaluations/{list,form}`.
- `EvaluationTest` (7). Tổng: **96/96 xanh**.

### 7.10 GĐ9 — Tổng hợp & Công bố kết quả (đã hoàn thành)
- `service/ResultService`: yêu cầu HĐ `FINISHED` + tất cả phân công đã `COMPLETED`; tính điểm TB từ các evaluation, làm tròn 2 chữ số; PASSED nếu ≥ `app.passing-score=5.00`, ngược lại FAILED; publish/unpublish cập nhật group → `COMPLETED`.
- Controller: `/results/manage` (Trưởng khoa), `/results/me` (Sinh viên).
- Template: `results/{manage,me}`.
- `ResultTest` (6). Tổng: **102/102 xanh**.

### 7.11 GĐ10 — Thông báo (đã hoàn thành)
- `service/NotificationService`: tạo/sửa/xoá (chỉ khi DRAFT), publish/hide; target role rỗng = tất cả vai trò, ngược lại chỉ gửi tới role trong danh sách.
- `dto/NotificationForm`, `controller/NotificationController` (`/notices`, `/notices/manage/**`).
- Template: `notices/{list,detail,form,manage}`.
- Sidebar có menu "Thông báo" + "Quản lý thông báo".
- `NotificationTest` (10). Tổng: **112/112 xanh**.

### 7.12 GĐ11 — Dashboard theo vai trò & DevTools (đã hoàn thành)
- Mở rộng `DashboardService.todoFor` cho 4 vai trò:
  - **TRUONG_KHOA**: "Đề tài chờ duyệt" (count PENDING topics).
  - **GIANG_VIEN** / **CHU_TICH_HOI_DONG**: "Nhóm chờ duyệt đăng ký" (JPQL join `topic_supervisors`, không dùng lazy stream).
  - **SINH_VIEN**: "Đăng ký đề tài cho nhóm của bạn" nếu có nhóm DRAFT.
- `latestNotices` hỗ trợ thông báo không target role (hiện cho tất cả).
- `DashboardTest` (6). Tổng: **118/118 xanh**.

### 7.13 GĐ12 — Rà soát toàn diện & Tài liệu (đã hoàn thành)
- `docs/postman/HT_QLDT.postman_collection.json`: ~50 endpoint xuyên suốt 13 giai đoạn.
- `README.md` cập nhật trạng thái, hướng dẫn chạy demo, tổng kết test.
- Cấu trúc dự án ổn định, `./mvnw clean test` xanh 118/118.

### Kết quả `./mvnw test` cuối cùng

```
Tests run: 118, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

| Test class          | Pass | Tổng |
|---------------------|:---:|:---:|
| CouncilTest         | 10  | 10  |
| DashboardTest       |  6  |  6  |
| EvaluationTest      |  7  |  7  |
| GroupRegistrationTest | 21 | 21 |
| NotificationTest    | 10  | 10  |
| RegistrationPeriodTest | 13 | 13 |
| ReportSubmissionTest |  6 |  6 |
| ResultTest          |  6  |  6  |
| SecurityAccessTest  |  7  |  7  |
| StaticBootstrapTest |  2  |  2  |
| TopicTest           | 18  | 18  |
| UserManagementTest  | 12  | 12  |
| **Tổng**            |**118**|**118**|

---

## 8. Cần tôi xác nhận

| Mã | Điểm chưa rõ | Đã chọn tạm |
|----|-------------|-------------|
| A1 | Ai duyệt đề tài | Trưởng khoa (`TOPIC_APPROVE`) |
| A2 | Ai công bố kết quả | Trưởng khoa (`RESULT_PUBLISH`) |
| A3 | Ai lập hội đồng | Trưởng khoa (`COUNCIL_MANAGE`) |
| A4 | Thang điểm / ngưỡng | 0–10, `HALF_UP` 2 chữ số, ĐẠT ≥ 5,00 (cấu hình `app.passing-score`) |
| A5 | GVHD thứ hai cùng bộ môn? | Không bắt buộc |
| A6 | Hội đồng áp dụng cho loại nào | Mọi loại; `review_deadline` chỉ TLCN/KLTN; `council_report_date` chỉ KLTN |
| A7 | Nộp báo cáo | File PDF/DOC/DOCX ≤ 20MB, version tăng dần, chỉ nhóm trưởng, chỉ khi nhóm `APPROVED` |
| A8 | Đề tài từ chối | GV sửa & gửi duyệt lại (`REJECTED → PENDING`), có lý do |

---

## 9. Rủi ro & việc còn lại

- **Spring Boot 4.1.1 vs 3.5.x**: đề bài nhắc 4.1.1 nhưng phiên bản này chưa phát hành ổn định. Tôi dùng 3.5.x (LTS-ish) cho GĐ1–GĐ2; khi Spring Boot 4.1.1 ổn định, nâng cấp bằng cách đổi `<parent>` là xong.
- **Maven Wrapper JAR**: chưa commit file binary. Cần chạy `mvn -N wrapper:wrapper` ở môi trường phát triển để sinh.
- **CSRF + REST API**: hiện đang bỏ qua CSRF cho `/api/**` để chừa chỗ cho các REST API tương lai (SecurityApiController theo đề bài). Khi viết REST API sẽ thay bằng bearer-token / API key riêng, không dùng session.
- **H2 vs MySQL**: test dùng H2 ở mode MySQL + `ddl-auto=create-drop` để không phụ thuộc MySQL server. Khi chạy production, schema do `HT_QLDT.sql` quản lý, `ddl-auto=validate`. `BaseEntity` thêm `@PrePersist` để tự set `created_at`/`updated_at` khi DB thiếu default (H2). Trên MySQL production, các cột đã có `DEFAULT CURRENT_TIMESTAMP` nên an toàn cả hai phía.
- **GĐ 3–12**: lần lượt bổ sung Đợt đăng ký, Đề tài, Nhóm & đăng ký, Báo cáo, Hội đồng, Phân công chấm & nhập điểm, Tổng hợp & công bố, Thông báo, Dashboard, rà soát cuối.