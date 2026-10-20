# Kế hoạch triển khai 13 giai đoạn

Theo đúng đặc tả mục 9 của đề bài. Trạng thái cập nhật đến Giai đoạn 1.

## Bảng tổng hợp

| GĐ | Nội dung | Trạng thái | Tệp chính đã thêm / sửa |
|----|----------|-----------|-------------------------|
| 0  | Xác minh công nghệ | ✅ Hoàn thành | README §6 (workspace trống → xin phép bootstrap) |
| 1  | Nền tảng: schema + security + layout + CSS + JS + dashboard rỗng | ✅ Hoàn thành | `pom.xml`, `application*.properties`, `docs/sql/HT_QLDT.sql`, `docs/seed/HT_QLDT_demo.sql`, 18 entity + 19 repo, security stack, layout Thymeleaf, 3 CSS, 1 JS, test cơ bản |
| 2  | Tài khoản, vai trò, phân quyền, đổi mật khẩu, nhật ký | ✅ Hoàn thành | `domain/BaseEntity`, `service/{User,Role,Permission,Password}Service`, `audit/AuditLogger`, `controller/admin/{User,Role,Permission}AdminController`, `controller/MeController`, templates `admin/*` & `me/*`, `UserManagementTest` (12) + `SecurityAccessTest` (7). `./mvnw test` xanh 21/21. |
| 3  | Đợt đăng ký | ✅ Hoàn thành | `RegistrationPeriodRepository` (search/findOpen*/hasAnyTopic), `RegistrationPeriodService` (validate thời gian + loại, publish/close/delete theo trạng thái), `RegistrationPeriodAdminController` + 3 template (`list/form/detail`), `RegistrationPeriodTest` (13 case). Sidebar trỏ `/admin/periods`. `./mvnw test` xanh 34/34. |
| 4  | Đề tài (GV đề xuất, Trưởng khoa duyệt, 1–2 GVHD, công bố, lọc theo bộ môn) | ✅ Hoàn thành | Mở rộng `TopicRepository` (search/findByPeriod/hasApprovedRegistration), `TopicSupervisorRepository`, `LecturerRepository` (findActiveByDepartmentId), `dto/TopicForm`, `service/TopicService` (create/update/approve/reject/publish/publishByPeriod/delete + kiểm tra khung GV, bộ môn, 1–2 GVHD không trùng, mã duy nhất, mô tả ≥ 30 ký tự), `controller/TopicLecturerController` (CRUD GV), `controller/admin/TopicAdminController` (duyệt/từ chối/công bố theo đợt/xóa), 4 template (`admin/topics/{list,detail}` + `topics/{list,form,detail}`), `TopicTest` (18 case). `./mvnw test` xanh 52/52. |
| 5  | Nhóm SV & đăng ký đề tài (cùng lớp) | ✅ Hoàn thành | Entity `StudentGroup` / `GroupMember` / `TopicRegistration`; repo `StudentGroupRepository` (search/findByMember/findWithDetails/findGroupsForPrimarySupervisor), `GroupMemberRepository`, `TopicRegistrationRepository` (findActiveByTopic/count/topic uniqueness); DTO `StudentGroupForm/AddMemberForm/TopicRegistrationForm/RejectRegistrationForm`; `service/StudentGroupService` (tạo nhóm DRAFT, thêm/xoá/đổi trưởng thành viên, enforce **cùng lớp** + 1–3 SV + unique group/period, khoá khi APPROVED); `service/RegistrationService` (đăng ký/đổi/huỷ, duyệt/từ chối với lý do ≥ 10 ký tự, chỉ GVHD chính `supervisor_order=1`, đề tài APPROVED+published, 1 topic ↔ 1 group); `controller/GroupController` (`/groups` cho SV) + `controller/RegistrationApprovalController` (`/registrations` cho GVHD); 5 template Thymeleaf (`groups/{list,form,detail}` + `registrations/{list,detail}`); sidebar có 2 menu: "Nhóm & đăng ký" (SV) và "Duyệt đăng ký" (GVHD); `DashboardService.todoFor` thêm TODO cho GVHD (nhóm chờ duyệt) và SV (đăng ký đề tài); `GroupRegistrationTest` 21 case. **Đã pin Lombok 1.18.32 (tương thích Java 21) trong `pom.xml`**. `./mvnw test` xanh 73/73. |
| 6  | Nộp báo cáo | ✅ Hoàn thành | `service/ReportService` (file upload + versioning `SUBMITTED/SUPERSEDED`, max 20MB, .pdf/.doc/.docx, leader-only, nhóm APPROVED/SUBMITTED), `controller/ReportController` (`/reports/**`), templates `reports/{index,list,upload}`, `ReportSubmissionTest` (6). `./mvnw test` xanh 79/79. |
| 7  | Hội đồng | ✅ Hoàn thành | `service/CouncilService` (CRUD + thành viên 3..5 người, role CHAIRMAN/SECRETARY/MEMBER, gán/bỏ đề tài, lifecycle `DRAFT→READY→FINISHED`), `controller/CouncilAdminController` (`/councils/**`), templates `councils/{list,form,detail}`, `CouncilTest` (10). `./mvnw test` xanh 89/89. |
| 8  | Phân công & nhập điểm | ✅ Hoàn thành | `service/EvaluationService` (assign / removeAssignment / submitEvaluation, score 0–10 HALF_UP 2 chữ số, comment ≥ 10 ký tự, 1 GV chỉ chấm 1 lần / đề tài trong 1 HĐ), `controller/EvaluationController` (`/assignments/**`, `/evaluations/**`), templates `assignments/list` + `evaluations/{list,form}`, `EvaluationTest` (7). `./mvnw test` xanh 96/96. |
| 9  | Tổng hợp & công bố | ✅ Hoàn thành | `service/ResultService` (tính điểm TB từ evaluation, PASSED/FAILED theo `app.passing-score=5.00`, publish/unpublish, COMPLETED group), `controller/ResultController` (`/results/manage`, `/results/me`), templates `results/{manage,me}`, `ResultTest` (6). `./mvnw test` xanh 102/102. |
| 10 | Thông báo | ✅ Hoàn thành | `service/NotificationService` (create/update/publish/hide/delete, target theo role, draft chỉ xem được nội bộ, published → tất cả role hoặc lọc theo target), `dto/NotificationForm`, `controller/NotificationController` (`/notices`, `/notices/manage/**`), templates `notices/{list,detail,form,manage}`, sidebar có menu "Thông báo" + "Quản lý thông báo", `NotificationTest` (10). `./mvnw test` xanh 112/112. |
| 11 | Dashboard theo vai trò + DevTools | ✅ Hoàn thành | Bổ sung `DashboardService` TODO cho 4 vai trò (TRUONG_KHOA: đề tài chờ duyệt; GVHD: nhóm chờ duyệt; SV: nhắc đăng ký; ADMIN: tổng quan) — sửa `latestNotices` để thông báo không target role cũng hiện cho tất cả, thêm `StudentGroupRepository.countByPrimarySupervisorAndStatus` (JPQL join `topic_supervisors` thay vì lazy stream). `DashboardTest` (6). `./mvnw test` xanh 118/118. |
| 12 | Rà soát toàn diện, Postman collection | ✅ Hoàn thành | Postman collection `docs/postman/HT_QLDT.postman_collection.json` (~50 endpoint từ auth → đợt → đề tài → nhóm → báo cáo → hội đồng → phân công → điểm → kết quả → thông báo). README cập nhật. `./mvnw test` xanh 118/118. |

## Nguyên tắc triển khai mỗi giai đoạn
1. Viết test cho các quy tắc nghiệp vụ (JUnit + Mockito + `@DataJpaTest`/`@SpringBootTest` + `@WithMockUser`).
2. Viết service và repository (nếu cần).
3. Viết controller + form/DTO.
4. Build: `./mvnw test` → xanh.
5. Kiểm tra giao diện bằng Chrome DevTools (3 độ rộng, Console sạch).
6. Cập nhật bảng trên và README.

## Phương pháp xác minh dev-tools (mục 8.6) — dùng ở GĐ11
- 1366px / 768px / 375px: không cuộn ngang trang; bảng rộng cuộn ngang trong khung riêng.
- Console: không lỗi đỏ, không cảnh báo vàng.
- Network: không 404 cho asset.
- Lighthouse (Accessibility): ≥ 90.

## Khi muốn commit
Sẽ tách mỗi giai đoạn thành **một commit** với thông điệp dạng `gd-02: users/roles/permissions modules`. Trước khi commit tôi sẽ hỏi ý kiến bạn (đề bài: "Không commit/push nếu tôi chưa yêu cầu").

## Phụ thuộc giữa các giai đoạn
- GĐ2 phụ thuộc GĐ1 (entity User/Role/Permission có sẵn) ✅
- GĐ3 dùng lại User của GĐ1 ✅
- GĐ4 dùng lại Department/Lecturer/Period ✅
- GĐ5 dùng lại Topic/Student/Group ✅
- GĐ6 dùng lại Group + Report (entity mới)
- GĐ7 dùng lại Period + Lecturer + Topic
- GĐ8 dùng lại Council + Evaluation
- GĐ9 dùng lại Evaluation + Result
- GĐ10 dùng lại Notification (entity có sẵn)
- GĐ11 dùng lại mọi dịch vụ trên

Vì vậy việc viết đầy đủ entity ở GĐ1 giúp GĐ2 trở đi chỉ tập trung vào nghiệp vụ, không phải Hibernate-validate lại schema.

## Báo cáo Giai đoạn 2 (chi tiết)

### Quy tắc nghiệp vụ đã enforce (mục 5.1)
- Tài khoản `LOCKED`/không `enabled` không đăng nhập được, thông báo chung (`Bad credentials`).
- Mật khẩu băm bằng `DelegatingPasswordEncoder` (BCrypt) — `UserService.create` mã hoá trước khi lưu.
- Đổi mật khẩu yêu cầu mật cũ đúng + mật mới ≥ 8 ký tự có cả chữ và số.
- 5 lần đăng nhập sai liên tiếp → khoá 15 phút (`LoginAttemptService`, có thể cấu hình qua `app.security.login.*`).
- Admin không thể tự khoá tài khoản mình, không thể bỏ vai trò `ADMIN` của admin cuối cùng.
- Vai trò hệ thống (`ADMIN`, `TRUONG_KHOA`, `CHU_TICH_HOI_DONG`, `GIANG_VIEN`, `SINH_VIEN`) không xoá được; danh mục quyền + ánh xạ vai trò-quyền khởi tạo **idempotent** khi app start (`DataBootstrapConfig`).
- Mọi thao tác ghi (POST) đều có `@PreAuthorize`; CSRF bật cho `/admin/**` và `/me/**`.
- Tất cả hành động nhạy cảm (tạo/khoá user, đổi role, reset/đổi mật khẩu, gán quyền) ghi `audit_logs` qua `AuditLogger`.

### Test bằng chứng
- `UserManagementTest` (12) — happy path + 4 ca từ chối (tự khoá, bỏ admin cuối, role hệ thống, mật khẩu cũ sai).
- `SecurityAccessTest` (7) — 2 ca 200, 1 ca 302 về `/login`, 1 ca 403 với lecturer, 2 ca kiểm tra CSRF, 1 ca ROLE_MANAGE.

### Kết quả build cuối GĐ2
```
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Báo cáo Giai đoạn 3 (chi tiết)

### Quy tắc nghiệp vụ đã enforce (mục 5.2)
- `lecturerStartAt < lecturerEndAt` và `studentStartAt < studentEndAt`.
- `studentStartAt >= lecturerEndAt` (hai giai đoạn riêng biệt).
- `reviewDeadline`: bắt buộc với `TLCN`/`KLTN`; nếu có thì phải sau `studentEndAt`.
- `councilReportDate`: chỉ có ở `KLTN`; nếu có thì phải sau `reviewDeadline`.
- Trạng thái: `DRAFT → PUBLISHED → CLOSED` (chỉ đi tới, không có rollback).
- `PUBLISHED`: chỉ sửa mốc thời gian, không đổi loại, không đổi tên.
- `DRAFT`: nếu đã có đề tài (Topic) thì không đổi loại.
- Xoá: chỉ `DRAFT` và chưa có đề tài.
- Mọi thao tác ghi ghi `audit_logs` (`PERIOD_CREATE/UPDATE/PUBLISH/CLOSE/DELETE`).
- `@PreAuthorize("hasAuthority('PERIOD_MANAGE')")` trên controller; sidebar menu chỉ hiện khi có quyền.
- JS ẩn/hiện trường theo loại đợt; **backend vẫn kiểm tra** ở service.

### Test bằng chứng
`RegistrationPeriodTest` (13 case):
- 3 happy path: `MON_HOC` (không có reviewDeadline), `TLCN` (có reviewDeadline), `KLTN` (có cả review + council).
- 7 ca từ chối:
  - `lecturerStart >= lecturerEnd`
  - `studentStart < lecturerEnd`
  - `MON_HOC` set `reviewDeadline`
  - `NCKH` set `councilReportDate`
  - `TLCN` thiếu `reviewDeadline`
  - `KLTN` `reviewDeadline` trước `studentEnd`
  - `KLTN` `councilReportDate` trước `reviewDeadline`
- 3 ca chuyển trạng thái: `PUBLISHED` đổi loại → từ chối; `CLOSED` cập nhật → từ chối; xoá `PUBLISHED` → từ chối.

### Kết quả build cuối GĐ3
```
[INFO] Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Báo cáo Giai đoạn 4 (chi tiết)

### Quy tắc nghiệp vụ đã enforce (mục 5.3)
- GV phải trong **khung thời gian GV** của đợt `PUBLISHED` mới được đề xuất/sửa đề tài (mọi lần đều kiểm tra ở service).
- Bộ môn của đề tài phải **trùng** bộ môn của GV tạo; không cho chọn bộ môn khác.
- **1 ≤ số GVHD ≤ 2**; trùng GVHD bị từ chối (cả 2 đều phải đang `ACTIVE` & `enabled`).
- Mô tả tối thiểu **30 ký tự** và tên tối thiểu **10 ký tự** (kiểm tra ở service phòng khi bypass Bean Validation).
- **Mã đề tài duy nhất** toàn hệ thống.
- Trạng thái: `PENDING → APPROVED` (kèm `approved_by`, `approved_at`) hoặc `PENDING → REJECTED` (kèm `reject_reason` ≥ 10 ký tự).
- Sau khi bị từ chối, GV có thể **sửa và gửi duyệt lại** (tự chuyển `REJECTED → PENDING`, xoá lý do cũ).
- Công bố: chỉ đề tài `APPROVED` mới được `published=true`; **một đề tài chỉ công bố 1 lần**.
- Không cho sửa/xoá đề tài đã `APPROVED` nếu có nhóm thực hiện (`hasApprovedRegistration`).
- Sửa/xoá đề tài chỉ dành cho **GV tạo** (kiểm tra quyền sở hữu).
- Mọi thao tác ghi ghi `audit_logs` (`TOPIC_CREATE/UPDATE/APPROVE/REJECT/PUBLISH/PUBLISH_BATCH/DELETE`).
- `@PreAuthorize("hasAuthority('TOPIC_CREATE'|'TOPIC_APPROVE')")` trên controller; sidebar menu "Đề tài" hiện cho `TOPIC_VIEW/CREATE/APPROVE`.
- Có màn hình riêng cho Trưởng khoa (`/admin/topics`): lọc theo bộ môn, đợt, trạng thái, đã công bố; **công bố cả đợt** (`/admin/topics/publish-by-period`).

### Test bằng chứng
`TopicTest` (18 case):
- 7 happy path: tạo 1 GVHD, tạo 2 GVHD, sửa `PENDING`, sửa `REJECTED` → `PENDING`, duyệt, từ chối, công bố.
- 11 ca từ chối:
  - Mã đề tài trùng.
  - Mô tả < 30 ký tự.
  - Trùng GVHD (1 == 2).
  - Bộ môn không khớp với GV.
  - Ngoài khung thời gian GV (đợt đã đóng).
  - Lý do từ chối < 10 ký tự.
  - Từ chối đề tài không ở `PENDING`.
  - Sửa đề tài không thuộc sở hữu.
  - Sửa đề tài `APPROVED` không có đăng ký (được phép).
  - Công bố 2 lần liên tiếp.
  - Xoá đề tài `APPROVED`.

### Kết quả build cuối GĐ4
```
[INFO] Tests run: 52, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```