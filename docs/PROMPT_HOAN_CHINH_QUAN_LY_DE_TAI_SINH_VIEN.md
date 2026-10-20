# PROMPT HOÀN CHỈNH: TRIỂN KHAI CHỨC NĂNG VÀ GIAO DIỆN HỆ THỐNG QUẢN LÝ ĐỀ TÀI SINH VIÊN

> **Dùng cho:** Cursor Agent, Claude Code, Codex hoặc AI lập trình trong VS Code.  
> **Repository mục tiêu:** `dynkayn/Student-Project-Management-System`, nhánh `main`.  
> **Tên hệ thống:** Hệ thống quản lý đề tài sinh viên – Khoa Công nghệ thông tin.  
> **Cách thực hiện:** làm theo từng giai đoạn, kiểm thử và báo cáo sau mỗi giai đoạn. Không commit/push khi chưa được tôi cho phép.

---

## 0. VAI TRÒ, MỤC TIÊU VÀ NGUYÊN TẮC ƯU TIÊN

Bạn là **kỹ sư full-stack Java/Spring Boot senior kiêm chuyên viên UI/UX**. Hãy đọc, kiểm tra và triển khai hệ thống quản lý đề tài sinh viên từ đầu đến cuối, bao gồm cơ sở dữ liệu, entity, repository, service, controller, phân quyền, kiểm thử, giao diện Thymeleaf, CSS, JavaScript, tài liệu và dữ liệu demo.

### Mục tiêu cần đạt

1. Hệ thống thực hiện đầy đủ quy trình quản lý đề tài môn học, nghiên cứu khoa học (NCKH), tiểu luận chuyên ngành (TLCN), khóa luận tốt nghiệp (KLTN).
2. Mọi quy tắc nghiệp vụ được kiểm tra ở **backend/service**, không chỉ kiểm tra bằng giao diện.
3. Giao diện tiếng Việt có dấu, hiện đại, nhất quán, dễ dùng, responsive, có điều hướng theo vai trò và hỗ trợ người dùng hoàn tất quy trình.
4. Dữ liệu trong database, entity, DTO, service, controller, template, test và tài liệu phải nhất quán.
5. Có thể cài đặt và chạy theo hướng dẫn trong README trên môi trường Java/MySQL phù hợp.

### Thứ tự ưu tiên nguồn thông tin

- **Nguồn nghiệp vụ gốc:** tài liệu đề bài “Hệ thống quản lý đề tài sinh viên” do tôi cung cấp.
- **Đặc tả triển khai trong prompt này:** các quy tắc kỹ thuật, dữ liệu, chức năng và UI/UX dưới đây.
- **Mã nguồn hiện tại:** dùng để hiểu cấu trúc và tái sử dụng phần đang đúng; không mặc định mọi mã hiện tại đều đúng.

Nếu phát hiện mâu thuẫn giữa code cũ và đề bài, ưu tiên đề bài; ghi rõ mâu thuẫn, tác động và phương án xử lý trong báo cáo. Không âm thầm bỏ yêu cầu, tự thay nghiệp vụ hoặc tự thêm chức năng ngoài phạm vi. Nếu gặp điểm chưa rõ, áp dụng giả định trong mục 3 và đưa vào danh sách cần tôi xác nhận.

### Nguyên tắc thay đổi mã nguồn

- “Triển khai lại từ đầu” nghĩa là sửa/viết lại các lớp và giao diện cần thiết, **không có nghĩa thay đổi bộ công nghệ**.
- Giữ framework, database, package gốc `blog.hethong_quanlydetai`, cấu hình kết nối phù hợp và Maven Wrapper hiện có, trừ khi kiểm tra cho thấy cần hỏi tôi trước.
- Đọc code trước khi sửa; không xóa file khi chưa xác định nơi sử dụng và chưa chuyển đổi, kiểm tra xong.
- Giữ URL, tên field form, thuộc tính model và quy ước phân quyền hiện hữu nếu tương thích. Nếu buộc phải thay đổi, cập nhật đồng bộ tất cả liên kết, template, test và tài liệu, đồng thời ghi lại lý do.
- Không chạy lệnh phá hủy dữ liệu hoặc lịch sử Git; không dùng `git reset --hard`, không tự ý xóa database thực, không commit/push khi chưa được cho phép.
- Làm theo giai đoạn. Sau mỗi giai đoạn phải build, test, kiểm tra phần đã làm và báo cáo ngắn gọn. **Dừng và chờ tôi xác nhận “TIẾP TỤC” trước khi chuyển sang giai đoạn kế tiếp.**

---

## 1. KIỂM TRA DỰ ÁN TRƯỚC KHI VIẾT CODE

Ở giai đoạn đầu, hãy đọc và xác minh thực tế các nội dung sau:

1. `pom.xml`: phiên bản Spring Boot, `java.version`, các dependency đang có; chú ý Spring Web/MVC, Data JPA, Security, Validation, Thymeleaf, MySQL Connector, Lombok, DevTools và test.
2. Chạy `java -version` và `./mvnw -v` để xác minh JDK dùng để build. Yêu cầu mục tiêu là **Java 21**. Nếu cấu hình dự án không dùng Java 21 hoặc môi trường không có JDK 21, dừng và báo tôi, không tự thay đổi version.
3. Đọc `application.properties`/`application.yml`, cấu hình database, `ddl-auto`, `open-in-view`, logging, upload file, security và các biến môi trường.
4. Kiểm tra package Java, controller, service, repository, entity, DTO/form, validation, exception handler và cấu hình Spring Security.
5. Kiểm tra toàn bộ `templates/`, `static/`, CSS/JS, các fragment Thymeleaf, đường dẫn form, tên thuộc tính model và menu hiện tại.
6. Kiểm tra test hiện có bằng `./mvnw test` trước khi thay đổi; ghi lại lỗi nền đã tồn tại.
7. Kiểm tra `docs/`, `README.md`, script SQL, các file MySQL Workbench/Postman và dữ liệu demo.
8. Xác định các màn hình, chức năng đang hoạt động, chưa hoạt động hoặc đang sai nghiệp vụ.

Tạo bảng báo cáo **“Dự kiến – Thực tế – Kết luận/Việc cần làm”**. Không tuyên bố đã kiểm tra thứ gì nếu chưa thực sự chạy hoặc đọc nó.

---

## 2. CÔNG NGHỆ VÀ GIỚI HẠN KỸ THUẬT

| Hạng mục | Yêu cầu |
|---|---|
| IDE | VS Code/Cursor |
| Backend | Java 21, Spring Boot theo phiên bản hiện có của dự án |
| Web | Spring MVC, controller render giao diện server-side |
| Giao diện | Thymeleaf, HTML5, CSS thuần, JavaScript thuần (vanilla JS) |
| Bảo mật | Spring Security, method security, CSRF |
| ORM | Spring Data JPA/Hibernate |
| Database | MySQL, quản lý bằng MySQL Workbench |
| Build | Maven hoặc Maven Wrapper hiện có (`./mvnw`) |
| Test | JUnit, Spring Boot Test, Mockito; `spring-security-test` nếu đã có hoặc được chấp thuận |
| Kiểm tra UI | Google Chrome DevTools, Responsive mode, Lighthouse |
| API testing | Postman chỉ cho endpoint REST thực sự tồn tại |

### Những điều không được tự ý làm

- Không chuyển sang Node.js, .NET, Python backend, React, Angular, Vue hoặc SPA framework.
- Không đổi MySQL sang SQLite/PostgreSQL hoặc một database khác.
- Không thêm Bootstrap, Tailwind, jQuery, thư viện icon, thư viện UI lớn, font/CDN bên ngoài khi CSS/SVG thuần đủ dùng.
- Không thêm ORM, thư viện upload, validation, ngày giờ hoặc thư viện khác trùng chức năng đã có.
- Chỉ thêm dependency mới khi thực sự cần thiết; trước khi thêm phải nêu lý do và chờ tôi đồng ý.
- Dùng SVG nội tuyến hoặc CSS cho icon; không phụ thuộc tải tài nguyên từ CDN.
- Không tạo REST API mới chỉ để kiểm thử một ứng dụng chủ yếu render bằng Thymeleaf. Với endpoint REST đã tồn tại, bổ sung Postman collection và hướng dẫn CSRF khi phù hợp.

---

## 3. MÔ TẢ NGHIỆP VỤ VÀ GIẢ ĐỊNH

Hệ thống tự động hóa quy trình:

**Tạo đợt đăng ký → Giảng viên đề xuất đề tài → Trưởng khoa duyệt đề tài → Công bố danh sách đề tài → Sinh viên lập nhóm và đăng ký → Giảng viên hướng dẫn chấp thuận → Nhóm thực hiện, nộp báo cáo → Thành lập hội đồng phản biện → Phân công giảng viên chấm → Nhập điểm → Chủ tịch hội đồng tổng hợp → Trưởng khoa công bố → Sinh viên xem kết quả.**

Mỗi đợt có 2 giai đoạn riêng:

- **Giai đoạn 1:** giảng viên đề xuất, hoàn thiện, duyệt và công bố danh sách đề tài.
- **Giai đoạn 2:** nhóm sinh viên đăng ký một đề tài đã được công bố.

Đợt có loại `MON_HOC`, `NCKH`, `TLCN`, `KLTN`. Trưởng khoa tạo đợt, khai báo thời gian giảng viên đăng ký và thời gian sinh viên đăng ký. Hạn chót GVPB chỉ áp dụng cho TLCN/KLTN; ngày báo cáo hội đồng chỉ áp dụng cho KLTN.

### Các giả định phải áp dụng nhất quán

- **A1 – Phê duyệt đề tài:** Trưởng khoa hoặc Admin, qua quyền `TOPIC_APPROVE`.
- **A2 – Công bố kết quả:** Trưởng khoa hoặc Admin, qua quyền `RESULT_PUBLISH`; Chủ tịch hội đồng chịu trách nhiệm tổng hợp điểm, không mặc định là người công bố.
- **A3 – Thành lập hội đồng:** Trưởng khoa hoặc Admin lập hội đồng, chọn thành viên/chủ tịch/thư ký. Chủ tịch hội đồng phân công người chấm cho hội đồng của mình.
- **A4 – Thang điểm:** 0–10; làm tròn `HALF_UP` 2 chữ số thập phân; đạt khi điểm cuối ≥ 5,00. Đưa ngưỡng đạt vào cấu hình `app.passing-score`.
- **A5 – GVHD thứ hai:** không bắt buộc cùng bộ môn với GVHD thứ nhất.
- **A6 – Áp dụng hội đồng:** hội đồng có thể sử dụng cho mọi loại đợt; hạn chót GVPB chỉ có ở TLCN/KLTN, ngày báo cáo hội đồng chỉ có ở KLTN.
- **A7 – Nộp báo cáo:** chỉ nhóm trưởng được nộp; cho phép PDF/DOC/DOCX, dung lượng tối đa 20 MB; lưu ngoài thư mục `static`, quản lý phiên bản tăng dần.
- **A8 – Đề tài bị từ chối:** giảng viên được sửa và gửi duyệt lại; bắt buộc ghi lý do từ chối.

Ghi các giả định này trong tài liệu cuối, nêu giả định nào đã dùng và phần nào còn cần tôi xác nhận.

---

## 4. VAI TRÒ, PHÂN QUYỀN VÀ QUYỀN SỞ HỮU

Vai trò tài khoản chính:

- `ADMIN`
- `TRUONG_KHOA`
- `CHU_TICH_HOI_DONG`
- `GIANG_VIEN`
- `SINH_VIEN`

Quyền kiểm tra bằng chuỗi authority, ví dụ `@PreAuthorize("hasAuthority('USER_MANAGE')")`. Không chỉ kiểm tra vai trò tổng quát; phải kiểm tra quan hệ với đối tượng cụ thể (đề tài do ai tạo/hướng dẫn, nhóm nào thuộc sinh viên, hội đồng nào do chủ tịch quản lý, người chấm được giao đề tài nào).

| Quyền | ADMIN | TRUONG_KHOA | CHU_TICH_HOI_DONG | GIANG_VIEN | SINH_VIEN |
|---|:---:|:---:|:---:|:---:|:---:|
| `USER_MANAGE`, `ROLE_MANAGE`, `PERMISSION_MANAGE` | ✓ | | | | |
| `PERIOD_MANAGE` | ✓ | ✓ | | | |
| `TOPIC_VIEW` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `TOPIC_CREATE` | | | | ✓ | |
| `TOPIC_APPROVE` | ✓ | ✓ | | | |
| `REGISTRATION_CREATE` | | | | | ✓ |
| `REGISTRATION_CONFIRM` | | | | ✓ | |
| `SUBMISSION_CREATE` | | | | | ✓ |
| `REPORT_VIEW` | ✓ | ✓ | ✓ | ✓* | ✓* |
| `COUNCIL_MANAGE` | ✓ | ✓ | | | |
| `ASSIGNMENT_MANAGE` | ✓ | | ✓* | | |
| `EVALUATION_CREATE` | | | ✓* | ✓* | |
| `RESULT_CALCULATE` | ✓ | | ✓* | | |
| `RESULT_PUBLISH` | ✓ | ✓ | | | |
| `RESULT_VIEW` | | | | | ✓* |
| `NOTICE_VIEW` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `NOTICE_MANAGE` | ✓ | ✓ | | | |

`*` Quyền bị giới hạn theo quan hệ sở hữu/phân công: giảng viên chỉ xem báo cáo của đề tài mình hướng dẫn hoặc được giao chấm; sinh viên chỉ xem báo cáo của nhóm mình; chủ tịch chỉ quản lý phân công/tổng hợp trong hội đồng mình làm chủ tịch; giảng viên/chủ tịch chỉ chấm đề tài được giao; sinh viên chỉ xem kết quả đã công bố của nhóm mình.

Lưu ý: **vai trò tài khoản** `CHU_TICH_HOI_DONG` khác với **vai trò thành viên trong từng hội đồng**. Một giảng viên có thể là chủ tịch một hội đồng và thành viên ở hội đồng khác. Mọi thao tác quản lý hội đồng phải kiểm tra đúng hội đồng liên quan.

Yêu cầu bảo vệ tài khoản và dữ liệu:

- `ADMIN` được toàn quyền nhưng không được tự khóa hoặc tự hạ quyền khiến mất quyền quản trị cuối cùng.
- Vai trò hệ thống gốc không được xóa. Không cho xóa/gỡ quyền của tài khoản Admin cuối cùng nếu dẫn đến hệ thống không còn Admin.
- Menu chỉ ẩn chức năng người dùng không có quyền; backend vẫn bắt buộc kiểm tra.
- Tất cả endpoint ghi dữ liệu phải có kiểm tra xác thực, authority và quyền sở hữu.

---

## 5. CHỨC NĂNG NGHIỆP VỤ CHI TIẾT

### 5.1. Đăng nhập, tài khoản cá nhân, tài khoản, vai trò và phân quyền

**Đăng nhập/đăng xuất**

- Form đăng nhập bằng Spring Security; cung cấp trạng thái lỗi tiếng Việt rõ ràng nhưng không tiết lộ tài khoản có tồn tại hay không.
- Tài khoản `LOCKED` hoặc `enabled=false` không đăng nhập được.
- Sau đăng nhập, điều hướng tới dashboard; hiển thị tên và vai trò hiện tại.
- Có hiện/ẩn mật khẩu; thông báo khi đăng xuất; CSRF vẫn bật.

**Quản lý tài khoản – `USER_MANAGE`**

- Danh sách tài khoản; tìm kiếm theo username, họ tên, email; lọc theo vai trò và trạng thái; phân trang.
- Tạo/sửa tài khoản; gán nhiều vai trò nếu mô hình hiện tại cho phép; khóa/mở khóa; đặt lại mật khẩu.
- Khi tạo tài khoản Sinh viên/Giảng viên, tạo đồng bộ hồ sơ `students`/`lecturers`; mã sinh viên/mã giảng viên phải duy nhất; hồ sơ giảng viên phải có bộ môn.
- Ngăn Admin tự khóa/hạ quyền chính mình và ngăn xóa quyền Admin cuối cùng.
- Hồ sơ cá nhân cho mọi người dùng; đổi mật khẩu phải nhập mật khẩu hiện tại. Mật khẩu mới tối thiểu 8 ký tự, có chữ và số.

**Vai trò và quyền**

- Xem danh sách, tạo/sửa vai trò, xem chi tiết quyền; gán/bỏ quyền cho vai trò bằng các checkbox có nhãn dễ hiểu.
- Các vai trò hệ thống gốc không thể xóa.
- Danh mục quyền mặc định được khởi tạo lặp lại an toàn (idempotent), không ghi đè các quyền Admin đã tùy chỉnh.

### 5.2. Quản lý đợt đăng ký – `PERIOD_MANAGE`

- Danh sách, chi tiết, tạo/sửa, công bố, đóng đợt. Lọc theo loại và trạng thái.
- Trường dữ liệu: tên đợt, loại đợt, thời gian GV bắt đầu/kết thúc, thời gian SV bắt đầu/kết thúc, hạn chót GVPB (chỉ TLCN/KLTN), ngày báo cáo hội đồng (chỉ KLTN), trạng thái, người tạo, thời điểm tạo.
- Trạng thái đợt: `DRAFT`, `PUBLISHED`, `CLOSED`.
- Kiểm tra thời gian bắt đầu < thời gian kết thúc; giai đoạn SV bắt đầu sau khi giai đoạn GV kết thúc; hạn chót GVPB sau thời điểm kết thúc đăng ký SV; ngày báo cáo hội đồng sau hạn chót GVPB khi có.
- Không nhận `review_deadline` cho `MON_HOC`/`NCKH`; không nhận `council_report_date` cho loại không phải `KLTN`.
- Chỉ cho sửa/xóa đợt khi chưa có đề tài. Đợt đã `PUBLISHED` chỉ sửa được mốc thời gian theo chính sách đã thống nhất, không đổi loại. Đợt `CLOSED` không cho thao tác đăng ký.
- Mọi thao tác đăng ký được backend kiểm tra theo giờ máy chủ và khung thời gian hợp lệ.

### 5.3. Quản lý đề tài – `TOPIC_VIEW`, `TOPIC_CREATE`, `TOPIC_APPROVE`

**Giảng viên đề xuất**

- Trong giai đoạn đăng ký GV của đợt `PUBLISHED`, giảng viên đề xuất đề tài gồm mã, tên, mô tả/nội dung rõ ràng, bộ môn, 1–2 GVHD.
- Bộ môn mặc định theo hồ sơ giảng viên, không được tự chọn bộ môn khác cho đề tài do mình tạo.
- Đề tài mới ở trạng thái `PENDING`. Người tạo được sửa đề tài `PENDING`/`REJECTED` của mình; đề tài bị từ chối phải hiển thị lý do và cho gửi duyệt lại.
- Không cho sửa đề tài đã `APPROVED` khi đã có đăng ký.

**Trưởng khoa/Admin duyệt đề tài**

- Hàng đợi đề tài chờ duyệt, tìm kiếm/lọc theo đợt, bộ môn, trạng thái.
- Duyệt hoặc từ chối. Khi từ chối bắt buộc nhập lý do; ghi người duyệt và thời điểm.
- Mã đề tài duy nhất; đề tài thuộc đúng một đợt và một bộ môn; có từ 1 đến 2 GVHD, không được trùng.
- Chốt cách công bố nhất quán: đề tài chỉ xuất hiện trong danh sách sinh viên đăng ký khi `status=APPROVED` và `published=true`. Có thao tác công bố danh sách theo đợt, hoặc tự động chuyển giai đoạn nếu triển khai theo cách đó; không sử dụng đồng thời hai cơ chế thiếu nhất quán.
- Danh sách đề tài cần hiển thị bộ môn, đợt, GVHD, trạng thái và nhóm đang thực hiện (nếu có).

### 5.4. Nhóm sinh viên và đăng ký đề tài – `REGISTRATION_CREATE`, `REGISTRATION_CONFIRM`

**Lập nhóm**

- Sinh viên lập nhóm trong giai đoạn SV của đợt `PUBLISHED`; người tạo tự trở thành nhóm trưởng.
- Tìm theo mã/tên để chọn tối đa 2 sinh viên khác; chỉ hiển thị người chưa thuộc nhóm nào.
- Mỗi nhóm tối đa 3 người, tối thiểu 1 người, đúng 1 nhóm trưởng; mỗi sinh viên chỉ thuộc duy nhất một nhóm trong suốt quá trình làm đề tài.
- Nhóm trưởng được thêm/bớt thành viên khi nhóm chưa có đăng ký đề tài hoạt động. Không cho thành viên thuộc nhóm khác, trùng thành viên hoặc nhóm vượt quá 3 người.

**Đăng ký đề tài**

- Chỉ nhóm trưởng được đăng ký; chỉ chọn đề tài đã công bố và thuộc đúng đợt của nhóm.
- Mỗi nhóm tối đa một đăng ký đang hoạt động (`PENDING`/`APPROVED`); một đề tài chỉ có tối đa một nhóm được chấp thuận.
- Có thể hủy khi đăng ký còn `PENDING`.

**Giảng viên hướng dẫn xử lý đăng ký**

- Chỉ GVHD thứ nhất hoặc thứ hai của đúng đề tài có quyền chấp thuận/từ chối.
- Chấp thuận phải chạy trong một transaction: đổi đăng ký thành `APPROVED`, gán `student_groups.topic_id`, cập nhật nhóm sang `APPROVED`, tự từ chối các đăng ký `PENDING` còn lại của cùng đề tài.
- Từ chối phải bắt buộc nhập lý do; nhóm có thể đăng ký đề tài khác sau khi đăng ký hiện tại không còn hoạt động.
- Xử lý trường hợp hai người đăng ký/duyệt đồng thời: transaction, constraint DB và bắt lỗi xung đột dữ liệu để không tạo dữ liệu trùng.

### 5.5. Nộp và xem báo cáo – `SUBMISSION_CREATE`, `REPORT_VIEW`

- Chỉ nhóm trưởng được nộp báo cáo và chỉ khi nhóm đã được chấp thuận đề tài.
- Thông tin gồm tiêu đề, file, mô tả, phiên bản, người nộp, thời điểm và trạng thái.
- Chỉ nhận PDF/DOC/DOCX, tối đa 20 MB; kiểm tra phần mở rộng và nội dung file khi khả thi; làm sạch tên file và lưu bằng tên ngẫu nhiên trong thư mục cấu hình `app.storage.dir`, **nằm ngoài `static`**.
- Phiên bản tăng dần theo từng nhóm; không cho trùng `(group_id, version)`.
- Nhóm chuyển `SUBMITTED` khi có báo cáo đầu tiên.
- Kiểm tra quyền ở cả chức năng xem và tải: Admin, Trưởng khoa, thành viên nhóm sở hữu, GVHD, thành viên hội đồng phụ trách đề tài theo đúng phạm vi. Không cung cấp URL tĩnh cho phép tải file không qua kiểm tra quyền.
- Danh sách hỗ trợ lọc theo đợt, nhóm, đề tài, trạng thái.

### 5.6. Thành lập hội đồng phản biện – `COUNCIL_MANAGE`

- Trưởng khoa/Admin tạo hội đồng theo đợt; mã và tên duy nhất theo quy tắc thiết kế; ngày báo cáo lấy từ cấu hình đợt KLTN khi áp dụng.
- Mỗi hội đồng có từ 3 đến 5 giảng viên, đúng 1 chủ tịch (`CHAIRMAN`) và đúng 1 thư ký (`SECRETARY`); các thành viên còn lại là `MEMBER`.
- Chỉ chọn các đề tài đã có báo cáo (`SUBMITTED`) và thuộc đúng đợt.
- Hội đồng chỉ chuyển sang `READY` khi thỏa mọi điều kiện thành viên/vai trò/đề tài. Không được xóa thành viên khiến hội đồng `READY` mất tính hợp lệ. Không xóa hội đồng đã có phân công hoặc điểm.
- Một GV có thể tham gia nhiều hội đồng, không được trùng thành viên trong cùng hội đồng.
- Trang chi tiết phải thể hiện thành viên, đề tài, phân công, tiến độ chấm và bảng kiểm tra hội đồng hợp lệ.

### 5.7. Phân công chấm và nhập điểm – `ASSIGNMENT_MANAGE`, `EVALUATION_CREATE`

**Phân công**

- Chủ tịch của đúng hội đồng hoặc Admin được phân công người chấm.
- Chỉ phân công khi hội đồng `READY`; người chấm phải là thành viên hội đồng; đề tài phải thuộc cùng đợt, đã có báo cáo và thuộc hội đồng.
- Người chấm không được là GVHD thứ nhất/thứ hai của đề tài và không được là giảng viên tạo đề tài nếu yêu cầu nghiệp vụ hiện tại quy định như vậy.
- Không tạo phân công trùng `(council_id, topic_id, lecturer_id)`; mỗi đề tài phải có tối thiểu một người chấm. Không xóa phân công đã có điểm.

**Nhập điểm**

- Chỉ giảng viên được phân công mới nhìn thấy và chấm đề tài được giao; kiểm tra tương tự ở backend.
- Điểm từ 0 đến 10, tối đa hai chữ số thập phân; nhận xét bắt buộc.
- Mỗi phân công có tối đa một bản chấm, được chỉnh sửa đến hạn chót phù hợp. Quá hạn thì từ chối, trừ Admin theo quy tắc ngoại lệ có ghi audit log.
- Hiển thị hạn chót và số ngày/thời gian còn lại trên màn hình nếu đợt có hạn chót.
- Trạng thái phân công thể hiện chưa chấm/đã chấm/quá hạn rõ ràng.

### 5.8. Tổng hợp và công bố kết quả – `RESULT_CALCULATE`, `RESULT_PUBLISH`, `RESULT_VIEW`

- Chủ tịch của đúng hội đồng hoặc Admin chỉ được tổng hợp khi **tất cả người chấm đã được phân công đều nộp điểm**.
- Công thức: điểm cuối = trung bình cộng điểm thành phần, làm tròn `HALF_UP` 2 chữ số thập phân. Kết quả `PASSED` nếu điểm cuối ≥ cấu hình `app.passing-score` (mặc định 5,00), ngược lại `FAILED`.
- Trước khi chốt, hiển thị bảng điểm và nhận xét của các thành viên. Nếu chưa công bố, có thể tổng hợp lại khi điểm thành phần thay đổi; sau khi công bố thì khóa chỉnh sửa điểm/kết quả.
- Trưởng khoa/Admin được công bố kết quả từng đề tài hoặc theo cả hội đồng; lưu người công bố và thời điểm.
- Sinh viên chỉ xem kết quả **đã công bố** của nhóm mình. Không hiển thị điểm thành phần của từng GV cho SV theo mặc định; hiển thị điểm cuối, Đạt/Không đạt, ngày công bố và nhận xét tổng hợp phù hợp.

### 5.9. Thông báo chính thức – `NOTICE_VIEW`, `NOTICE_MANAGE`

- Admin/Trưởng khoa tạo thông báo gồm tiêu đề, nội dung, loại `SCHOOL` (Nhà trường) hoặc `FACULTY` (Khoa), đối tượng nhận (một/nhiều vai trò hoặc tất cả), trạng thái `DRAFT`/`PUBLISHED`.
- Được sửa/xóa khi ở `DRAFT`; thông báo đã công bố chỉ được thu hồi/ẩn, không xóa hẳn khỏi lịch sử.
- Người dùng chỉ xem thông báo `PUBLISHED` có đối tượng nhận khớp vai trò của mình.
- Danh sách lọc theo loại, ngày, trạng thái khi người có quyền quản lý; phân trang; thông báo mới được hiển thị trên dashboard.

### 5.10. Dashboard/Tổng quan theo vai trò

Mọi người dùng xem thông báo mới, bước hiện tại của quy trình và khối **“Việc cần làm của tôi”** tương ứng. Chỉ lấy số liệu có sẵn; không tạo số liệu giả để lấp chỗ trống. Nếu backend chưa có dữ liệu cần thiết thì báo cáo và đề xuất phần cần bổ sung.

- **Admin:** tổng quan tài khoản, vai trò, đợt, đề tài, nhóm, báo cáo, hội đồng, kết quả; lối tắt quản trị.
- **Trưởng khoa:** đợt đang mở/sắp đóng, đề tài chờ duyệt, hội đồng chưa hợp lệ, kết quả chờ công bố.
- **Chủ tịch hội đồng:** đề tài chưa được phân công chấm, người chấm chưa hoàn tất, đề tài đủ điểm chờ tổng hợp.
- **Giảng viên:** đăng ký chờ xác nhận, đề tài mình hướng dẫn, đề tài cần chấm, hạn chót sắp đến.
- **Sinh viên:** trạng thái nhóm/đăng ký, báo cáo đã nộp, trạng thái đề tài, kết quả đã công bố.

Dashboard cần có lời chào/tên/vai trò, thanh tiến trình: **Tạo đợt → Đề xuất & duyệt đề tài → SV đăng ký → Thực hiện & nộp báo cáo → Phản biện & chấm → Công bố**; thông báo mới; thẻ số liệu phù hợp với quyền; bảng đợt đang mở; lối tắt chức năng.

---

## 6. THIẾT KẾ CƠ SỞ DỮ LIỆU VÀ TOÀN VẸN DỮ LIỆU

Thiết kế thống nhất theo schema trong mục này, không bê nguyên cấu trúc cũ nếu nó thiếu trường hoặc không đáp ứng nghiệp vụ. Tên bảng/cột dùng `snake_case`, có PK/FK/UNIQUE/CHECK rõ ràng và tên constraint dễ đọc. Charset `utf8mb4`. Tạo **một script nguồn chuẩn `HT_QLDT.sql`** có thể chạy trên database MySQL mới bằng MySQL Workbench. Schema chuẩn không chứa dữ liệu demo.

### 6.1. Nhóm bảng bắt buộc

**Tài khoản và phân quyền**

- `users`: `user_id`, `username` (UNIQUE), `password` (password hash), `full_name`, `email` (UNIQUE), `phone`, `status` (`ACTIVE`/`LOCKED`), `enabled`, `created_at`.
- `roles`: `role_id`, `name` (UNIQUE), `description`.
- `permissions`: `permission_id`, `code` (UNIQUE), `name`, `description`.
- `user_roles`: bảng nối người dùng–vai trò, khóa chính ghép `(user_id, role_id)`.
- `role_permissions`: bảng nối vai trò–quyền, khóa chính ghép `(role_id, permission_id)`.

**Bộ môn và hồ sơ**

- `departments`: `department_id`, `department_code` (UNIQUE), `department_name`, `description`, `status`.
- `students`: `student_id`, `user_id` (FK + UNIQUE), `student_code` (UNIQUE), `class_name`, `major`.
- `lecturers`: `lecturer_id`, `user_id` (FK + UNIQUE), `lecturer_code` (UNIQUE), `academic_degree`, `department_id` (FK).

**Đợt và đề tài**

- `registration_periods`: `period_id`, `period_name`, `period_type`, thời gian GV/SV, `review_deadline`, `council_report_date`, `status`, `created_by`, `created_at`.
- `topics`: `topic_id`, `period_id`, `department_id`, `created_by_lecturer_id`, `topic_code` (UNIQUE), `topic_name`, `description`, `status`, `reject_reason`, `approved_by`, `approved_at`, `published`, `created_at`.
- `topic_supervisors`: `topic_supervisor_id`, `topic_id`, `lecturer_id`, `supervisor_order`, `assigned_at`; UNIQUE `(topic_id, lecturer_id)` và UNIQUE `(topic_id, supervisor_order)`.

**Nhóm, đăng ký và báo cáo**

- `student_groups`: `group_id`, `period_id`, `group_code` (UNIQUE), `group_name`, `topic_id` NULL/UNIQUE (đề tài được chấp thuận hiện tại), `status`, `created_at`.
- `group_members`: `group_member_id`, `group_id`, `student_id`, `is_leader`, `joined_at`; UNIQUE `(student_id)` để mỗi SV chỉ thuộc một nhóm; UNIQUE `(group_id, student_id)`.
- `topic_registrations`: `registration_id`, `group_id`, `topic_id`, `status`, `registered_at`, `approved_by`, `approved_at`, `rejection_reason`. Điều kiện tối đa một đăng ký hoạt động cho nhóm và một nhóm được chấp thuận cho mỗi đề tài phải được bảo vệ ở service/transaction và constraint phù hợp.
- `reports`: `report_id`, `group_id`, `submitted_by`, `report_title`, `file_name`, `stored_path`, `file_size`, `version`, `description`, `submitted_at`, `status`; UNIQUE `(group_id, version)`.

**Hội đồng, chấm điểm và kết quả**

- `councils`: `council_id`, `period_id`, `council_code` (UNIQUE), `council_name`, `report_date`, `status`.
- `council_members`: `council_member_id`, `council_id`, `lecturer_id`, `role` (`CHAIRMAN`/`SECRETARY`/`MEMBER`), `joined_at`; UNIQUE `(council_id, lecturer_id)`.
- `council_topics`: liên kết đề tài thuộc hội đồng, có `council_id`, `topic_id` và ràng buộc `UNIQUE(topic_id)` để mỗi đề tài chỉ được gán vào một hội đồng trong mô hình này.
- `review_assignments`: `assignment_id`, `council_id`, `topic_id`, `lecturer_id`, `assigned_by`, `assigned_at`, `status`; UNIQUE `(council_id, topic_id, lecturer_id)`.
- `evaluations`: `evaluation_id`, `assignment_id` (UNIQUE), `score`, `comment`, `evaluated_at`, `status`.
- `review_results`: `result_id`, `group_id`, `topic_id` (UNIQUE), `council_id`, `final_score`, `result`, `calculated_by`, `calculated_at`, `published`, `published_at`, `published_by`.

**Thông báo và nhật ký**

- `notifications`: `notification_id`, `title`, `content`, `type` (`SCHOOL`/`FACULTY`), `created_by`, `status`, `created_at`, `published_at`, `updated_at`.
- `notification_target_roles`: khóa ghép `(notification_id, role_id)`.
- `audit_logs`: `audit_log_id`, `user_id`, `action`, `entity`, `entity_id`, `detail`, `created_at`.

### 6.2. Constraint, enum và điều kiện dữ liệu

- `period_type` chỉ nhận `MON_HOC`, `NCKH`, `TLCN`, `KLTN`; đợt bắt đầu < kết thúc; thời gian SV bắt đầu sau thời gian GV kết thúc.
- `review_deadline` chỉ khác NULL với TLCN/KLTN; `council_report_date` chỉ khác NULL với KLTN.
- Đề tài có 1–2 GVHD; `supervisor_order` chỉ nhận 1 hoặc 2.
- Nhóm có 1–3 SV và đúng 1 nhóm trưởng; mỗi SV tối đa một nhóm.
- Mỗi nhóm tối đa một đăng ký hoạt động; một đề tài tối đa một nhóm được chấp thuận.
- Hội đồng có 3–5 GV, đúng 1 chủ tịch và 1 thư ký.
- Người chấm phải thuộc hội đồng, không phải GVHD của đề tài; mỗi phân công tối đa một bản chấm.
- Điểm nằm trong [0, 10]; `reports.version > 0`; kết quả cuối nằm trong [0, 10].
- Chỉ tổng hợp khi đủ điểm; điểm cuối là trung bình cộng; đã công bố thì không được thay đổi điểm/kết quả.
- Tất cả quy tắc không thể bảo vệ bằng constraint thuần phải được kiểm tra tại service trong transaction; bổ sung test để chứng minh.

### 6.3. Tệp SQL, JPA và cấu hình

- `HT_QLDT.sql` là nguồn chuẩn để khởi tạo schema. Đặt tên constraint rõ ràng, kiểm tra script chạy sạch trên database mới và Reverse Engineer được EER trong Workbench.
- Cấu hình JPA dùng `ddl-auto=validate` hoặc `none`; không để Hibernate tự ý sửa schema ngoài kế hoạch.
- Entity/association phải ánh xạ đúng tất cả các trường và constraint SQL. Phát hiện mọi sai khác kiểu dữ liệu, cột, nullable, tên bảng/cột, unique và quan hệ trước khi bàn giao.
- Có script dữ liệu demo riêng hoặc Spring profile `demo`, không nhét dữ liệu demo vào schema chuẩn.
- Cập nhật ERD và tài liệu quan hệ theo schema mới; không coi ERD/schema cũ là chính xác nếu đã thay đổi mô hình.

---

## 7. YÊU CẦU KIẾN TRÚC BACKEND, BẢO MẬT VÀ KIỂM SOÁT LỖI

### 7.1. Phân lớp

Luồng kiến trúc: `Controller → Service → Repository → Database`.

- Controller chỉ nhận request, bind form/DTO, gọi service, trả view/redirect; không chứa logic nghiệp vụ phức tạp.
- Service chứa kiểm tra nghiệp vụ, quyền sở hữu cần thiết và `@Transactional` cho các thao tác nhiều bước.
- Repository dùng Spring Data JPA, truy vấn có tham số; không nối chuỗi truy vấn chứa input người dùng.
- Dùng DTO/form object để nhận dữ liệu, không bind trực tiếp request vào entity để tránh mass assignment.
- Dùng enum/hằng số tập trung cho trạng thái, loại đợt, vai trò trong hội đồng; tránh chuỗi hard-code rải rác.
- Dùng Bean Validation ở form và lặp lại kiểm tra nghiệp vụ bắt buộc tại service.
- Tránh N+1 bằng `@EntityGraph`/fetch join phù hợp; mọi danh sách dài cần phân trang; cấu hình `open-in-view=false` nếu tương thích với kiến trúc và xử lý tải dữ liệu rõ ràng.

### 7.2. Bảo mật

- Mật khẩu băm bằng `DelegatingPasswordEncoder`/BCrypt; không lưu mật khẩu thật trong repo, log hoặc SQL seed.
- Không dùng `{noop}` ngoài môi trường demo được cách ly.
- Bật CSRF cho mọi form POST; chống session fixation khi đăng nhập; giữ security headers mặc định và bổ sung nếu phù hợp.
- Có cơ chế khóa tạm theo tài khoản sau nhiều lần đăng nhập thất bại (ví dụ 5 lần trong 15 phút), không cần thư viện ngoài; ghi rõ cách đặt lại khóa.
- Escape mặc định của Thymeleaf; không dùng `th:utext` với nội dung người dùng.
- Tất cả thao tác ghi phải kiểm tra authority và quyền sở hữu; không chỉ dựa vào menu ẩn/hiện.
- Kiểm tra quyền khi tải file báo cáo; không expose đường dẫn thực tế lưu trữ.
- Trang 403/404/500 thân thiện, tiếng Việt; không hiển thị stack trace, secret hay thông tin nhạy cảm.
- Không ghi password, token, session id hoặc nội dung nhạy cảm vào `audit_logs`.

### 7.3. Cấu hình và môi trường

- Lấy `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` qua biến môi trường; không hard-code secret.
- Khai báo rõ `app.storage.dir`, `app.passing-score`, các cấu hình security và upload.
- Múi giờ hiển thị `Asia/Ho_Chi_Minh`; ngày giờ định dạng `dd/MM/yyyy HH:mm`; điểm hiển thị theo quy tắc tiếng Việt, ví dụ `8,17`.
- Logging ở mức hợp lý (`INFO`/`WARN`), không tắt toàn bộ logging.

### 7.4. Xử lý lỗi và ghi nhật ký

- Dùng `@ControllerAdvice`/handler phù hợp để xử lý validation, lỗi nghiệp vụ, truy cập bị từ chối, dữ liệu trùng và lỗi không tìm thấy.
- Thông báo lỗi tiếng Việt cụ thể và dễ hiểu; nếu lỗi thuộc trường nào phải hiển thị gần trường đó và giữ lại dữ liệu người dùng đã nhập khi có thể.
- Ghi `audit_logs` cho các thao tác trọng yếu: tạo/duyệt/từ chối đề tài; duyệt đăng ký; phân công; nhập/sửa điểm; tổng hợp; công bố/thu hồi kết quả; đổi quyền; khóa/mở khóa tài khoản; tải/xử lý tài liệu nếu phù hợp.

---

## 8. ĐỊNH HƯỚNG GIAO DIỆN VÀ TRẢI NGHIỆM NGƯỜI DÙNG (UI/UX)

### 8.1. Phong cách giao diện

Thiết kế theo phong cách **admin dashboard hiện đại, sạch, nhiều khoảng trắng, rõ thứ bậc, dễ thao tác**. Giao diện phải phù hợp ứng dụng quản lý trong khoa CNTT, không mang cảm giác trang landing page hoặc dashboard thương mại không liên quan.

- Dùng một khung trang chung: sidebar trái, topbar mỏng, vùng nội dung chính, flash message.
- Mỗi trang có breadcrumb, tiêu đề rõ ràng, mô tả ngắn, hành động chính dễ nhận biết.
- Nhóm chức năng theo tiến trình nghiệp vụ, mục đang chọn được tô sáng.
- Ưu tiên bảng dữ liệu dễ đọc và biểu mẫu có nhóm trường rõ ràng; không nhồi quá nhiều thẻ thống kê cùng lúc.
- Mọi trạng thái dùng chữ tiếng Việt và badge màu nhất quán; **không dùng màu làm tín hiệu duy nhất**.
- Nếu tôi cung cấp ảnh tham khảo, dùng ảnh đó làm chuẩn về bố cục/cảm giác thị giác nhưng vẫn giữ yêu cầu kỹ thuật, tương phản và luồng nghiệp vụ của prompt này.

### 8.2. Bảng màu và typography

Duy trì hệ màu nhận diện hiện có của dự án:

```css
:root {
  --navy: #173543;
  --teal: #087d70;
  --teal-soft: #e1f2ed;
  --orange: #c9613c;
  --yellow: #f5c85b;
  --sand: #f3f6f4;
  --paper: #ffffff;
  --line: #d8e2de;
  --muted: #60736f;
  --success: #18794e;
  --success-soft: #e6f4ec;
  --danger: #b42318;
  --danger-soft: #fcebea;
  --info: #175cd3;
  --info-soft: #eaf2ff;
  --warning: #8a6100;
  --warning-soft: #fff4cc;
}
```

- Dùng font hệ thống như `Segoe UI`, `system-ui`, `Arial`; không tải font từ CDN.
- Khoảng cách theo thang 4 px: 4, 8, 12, 16, 24, 32, 48 px.
- Bo góc: khoảng 8 px; card khoảng 12 px; shadow rất nhẹ.
- Cỡ chữ tham khảo: nội dung 15–16 px; tiêu đề trang 24–28 px; tiêu đề khối khoảng 18 px; chú thích tối thiểu 13 px.
- Đảm bảo tương phản văn bản/nền tối thiểu mục tiêu 4.5:1; hỗ trợ `:focus-visible` rõ ràng.
- Tránh màu gradient nặng, hiệu ứng quá nhiều, animation không cần thiết hoặc icon emoji làm biểu tượng chức năng chính.

### 8.3. Cấu trúc thư mục giao diện đề xuất

```text
src/main/resources/
├── templates/
│   ├── fragments/
│   │   ├── layout.html
│   │   ├── sidebar.html
│   │   └── components.html
│   ├── auth/login.html
│   ├── dashboard.html
│   ├── registration-periods/
│   ├── topics/
│   ├── registrations/
│   ├── reports/
│   ├── councils/
│   ├── review-assignments/
│   ├── evaluations/
│   ├── results/
│   ├── notifications/
│   ├── users/
│   ├── roles/
│   ├── permissions/
│   └── error/
└── static/
    ├── css/
    │   ├── theme.css
    │   ├── layout.css
    │   └── components.css
    └── js/
        └── app.js
```

Đây là cấu trúc mục tiêu; hãy đối chiếu với dự án thật trước khi di chuyển template. Không cần đổi URL chỉ để khớp cấu trúc thư mục.

- `theme.css`: biến thiết kế, reset, typography.
- `layout.css`: sidebar, topbar, main content, grid, responsive.
- `components.css`: nút, bảng, input, badge, card, modal, flash, phân trang và trạng thái rỗng.
- `app.js`: chỉ JS thuần, ngắn gọn, không phụ thuộc thư viện, hoạt động an toàn khi phần tử không có trên một trang.
- Mọi trang sau đăng nhập dùng cùng `fragments/layout.html`; login có layout riêng; trang lỗi có thiết kế phù hợp và không làm lộ lỗi kỹ thuật.

### 8.4. Menu điều hướng

Nhóm menu theo tiến trình và ẩn mục người dùng không có quyền; nếu cả nhóm không có mục nào được xem thì ẩn luôn tiêu đề nhóm:

1. **Tổng quan** – `/`
2. **Quy trình đề tài** – Đợt đăng ký `/registration-periods`; Đề tài `/topics`; Nhóm & đăng ký `/registrations`; Báo cáo `/reports`.
3. **Phản biện & chấm điểm** – Hội đồng `/councils`; Phân công chấm `/review-assignments`; Nhập điểm & tổng hợp `/evaluations`; Kết quả của tôi `/results`.
4. **Thông tin** – Thông báo `/notifications`.
5. **Quản trị** – Tài khoản `/users`; Vai trò `/roles`; Phân quyền `/permissions`; Hồ sơ cá nhân.

- Menu đang xem có trạng thái active.
- Sidebar desktop rộng khoảng 240–260 px; ở màn hình vừa có thể thu gọn; ở điện thoại chuyển thành drawer mở bằng nút `☰`.
- Topbar thể hiện tên người đăng nhập, vai trò và nút đăng xuất dạng POST có CSRF.
- Menu là yếu tố điều hướng duy nhất dùng chung; không chép lại sidebar riêng trong từng template.
- Do không mặc định có Thymeleaf Spring Security extras, hãy dùng các biến boolean/model attributes hoặc cơ chế tương đương đã có trong dự án; xác nhận dependency thực tế trước khi dùng `sec:authorize`.

### 8.5. Thành phần giao diện dùng chung

- **Page header:** breadcrumb, tên trang, mô tả ngắn và tối đa một nút hành động chính.
- **Button:** chính (teal), phụ (viền), nguy hiểm (đỏ), ghost; có hover/focus/disabled; vùng bấm tối thiểu khoảng 40 px, ưu tiên 44 px trên mobile.
- **Form:** nhãn phía trên input, `label for` khớp `id`, dấu `*` cho trường bắt buộc, gợi ý dưới trường, lỗi gần trường, giữ lại dữ liệu khi validation thất bại; các nhóm trường có tiêu đề.
- **Table:** tiêu đề cột rõ, hàng xen kẽ/hover nhẹ, cột thao tác gọn ở bên phải; bảng rộng cuộn ngang trong container riêng, không khiến toàn trang cuộn ngang.
- **Badge:** trạng thái chữ tiếng Việt + màu ngữ nghĩa thống nhất.
- **Flash message:** thành công, lỗi, thông tin; có nút đóng; tự ẩn thông báo thành công/thông tin sau khoảng 5 giây, không tự ẩn lỗi.
- **Empty state:** biểu tượng đơn giản, thông điệp hữu ích và hướng dẫn hành động tiếp theo.
- **Modal xác nhận:** dùng `<dialog>` hoặc thành phần accessible tương đương cho xóa, từ chối, khóa tài khoản, công bố/thu hồi, hoàn tất hội đồng. Không dùng `window.confirm`.
- **Pagination:** 10/20/50 dòng mỗi trang, hiển thị tổng bản ghi/trang hiện tại. Ưu tiên pagination backend; không tải tất cả dữ liệu về trình duyệt nếu có thể phát sinh danh sách lớn.
- **Progress stepper:** dùng trên dashboard và trang chi tiết phù hợp để giải thích đối tượng đang ở bước nào.
- **Icon:** SVG nội tuyến nhất quán, có `aria-hidden="true"` khi chỉ trang trí.

### 8.6. Chuẩn hiển thị trạng thái

Ánh xạ trạng thái theo ngữ cảnh, không hiện mã enum tiếng Anh thô trên giao diện:

| Mã/giá trị | Chữ hiển thị | Màu gợi ý |
|---|---|---|
| `DRAFT` | Nháp | Xám |
| `PUBLISHED` | Đã công bố | Xanh dương/xanh lá tùy ngữ cảnh |
| `CLOSED` | Đã đóng | Xám đậm |
| `PENDING` | Chờ duyệt / Chờ xác nhận | Vàng |
| `APPROVED` | Đã duyệt / Đã chấp thuận | Xanh lá |
| `REJECTED` | Từ chối | Đỏ |
| `CANCELLED` | Đã hủy | Xám |
| `SUBMITTED` | Đã nộp | Xanh dương |
| `ASSIGNED` | Đã phân công | Xanh dương |
| `COMPLETED` | Hoàn tất | Xanh lá |
| `PLANNED` | Dự kiến | Xám |
| `READY` | Sẵn sàng | Xanh lá |
| `FINISHED` | Đã kết thúc | Xám/xanh |
| `ACTIVE` | Đang hoạt động | Xanh lá |
| `LOCKED` | Đã khóa | Đỏ |
| `PASSED`/`PASS` | Đạt | Xanh lá |
| `FAILED`/`FAIL` | Không đạt | Đỏ |
| `MON_HOC` | Môn học | Nhãn loại |
| `NCKH` | Nghiên cứu khoa học | Nhãn loại |
| `TLCN` | Tiểu luận chuyên ngành | Nhãn loại |
| `KLTN` | Khóa luận tốt nghiệp | Nhãn loại |

Quét toàn bộ enum/trạng thái thực tế trong code và bổ sung ánh xạ còn thiếu. Không tự thay đổi mã trạng thái nghiệp vụ chỉ để tiện giao diện.

### 8.7. Responsive và khả năng truy cập

- ≥ 1200 px: sidebar cố định, nội dung có giới hạn chiều rộng hợp lý.
- 768–1199 px: sidebar thu gọn hoặc drawer.
- < 768 px: một cột, nút hành động xuống dưới tiêu đề, sidebar drawer, bảng cuộn ngang bên trong vùng bảng.
- Kiểm tra tại 1366 px, 768 px và 375 px; không có cuộn ngang toàn trang ở 375 px.
- `lang="vi"`, label cho tất cả input, thứ tự tab hợp lý, focus rõ, không sử dụng màu làm tín hiệu duy nhất, modal dùng được bằng bàn phím và đóng bằng Esc.

---

## 9. ĐẶC TẢ GIAO DIỆN CHO TỪNG MÀN HÌNH

Tất cả trang danh sách đều theo mẫu: **page header → khu tìm kiếm/lọc → nội dung bảng/thẻ → phân trang → empty state khi không có dữ liệu**. Tất cả danh sách phải kiểm tra quyền xem/thao tác ở backend. Mỗi bảng có trạng thái dạng badge và cột thao tác gọn nếu phù hợp.

### 9.1. Đăng nhập – `/login`

- Một card đặt giữa trang; nền sáng theo hệ màu; có nhận diện “ĐT – QUẢN LÝ ĐỀ TÀI / KHOA CNTT”.
- Hai trường username/password có label, nút hiện/ẩn mật khẩu, nút đăng nhập rõ ràng.
- Hiển thị lỗi đăng nhập chung, thông báo đăng xuất thành công; không có sidebar.
- Responsive, dễ sử dụng bàn phím.

### 9.2. Dashboard – `/`

- Lời chào, tên và vai trò người dùng.
- Thanh tiến trình quy trình đề tài của đợt hiện hành.
- Khối “Việc cần làm” theo vai trò, đặt nổi bật gần đầu trang.
- Thông báo mới, số liệu theo quyền, đợt đang mở, lối tắt thao tác.
- Không hiện khối thống kê không có dữ liệu; khi danh sách rỗng, có lời hướng dẫn rõ.
- Bố cục một cột chính và một cột phụ trên desktop, chuyển về một cột trên mobile.

### 9.3. Đợt đăng ký – `/registration-periods`, `/registration-periods/new`, `/{id}/edit`

**Danh sách:** lọc theo loại/trạng thái; các cột gồm tên và loại đợt, giai đoạn hiện tại (Chưa mở/GV đăng ký/SV đăng ký/Đã đóng), thời gian GV, thời gian SV, hạn chót/ngày báo cáo khi có, trạng thái và thao tác. Có Sửa/Công bố/Đóng/Xóa tùy quyền và trạng thái.

**Form:** chia thành các card/fieldset: Thông tin chung; Giai đoạn 1 – GV đăng ký; Giai đoạn 2 – SV đăng ký; Mốc chuyên môn. Dùng JS ẩn/hiện `review_deadline` khi loại là TLCN/KLTN và `council_report_date` khi loại là KLTN; khi ẩn trường thì xóa giá trị không hợp lệ. Backend phải kiểm tra lại mọi điều kiện thời gian và loại đợt.

### 9.4. Đề tài – `/topics`, `/topics/new`

**Danh sách:** tìm kiếm theo mã/tên/mô tả; lọc đợt, bộ môn, trạng thái. Mỗi dòng/thẻ hiển thị mã, tên, mô tả rút gọn có thể mở rộng, bộ môn, đợt, GVHD, trạng thái, nhóm đang thực hiện (nếu có).

- GV có quyền tạo đề tài thấy nút “Đề xuất đề tài”.
- Người có `TOPIC_APPROVE` thấy Duyệt/Từ chối, có modal xác nhận; Từ chối phải có ô lý do bắt buộc.
- SV chỉ thấy các đề tài đã duyệt và công bố có thể đăng ký; khi không được đăng ký phải hiển thị lý do/điều kiện, không chỉ vô hiệu hóa nút im lặng.

**Form:** đợt, mã, tên, nội dung/mô tả đầy đủ, bộ môn lấy từ hồ sơ GV, GVHD chính và GVHD thứ hai tùy chọn. Hiển thị chú thích đề tài mới sẽ ở trạng thái “Chờ duyệt”.

### 9.5. Nhóm và đăng ký – `/registrations`

Bố trí theo người dùng/role:

- **Sinh viên:** card “Nhóm của tôi” (thành viên, nhóm trưởng đánh dấu rõ, số lượng `x/3`, trạng thái); form lập nhóm; tìm nhanh SV chưa có nhóm; thêm/bớt thành viên khi đủ điều kiện; phần đăng ký đề tài chỉ dành cho nhóm trưởng. Hiển thị luồng trạng thái Chưa đăng ký → Chờ xác nhận → Đã chấp thuận/Từ chối.
- **Giảng viên:** hàng đợi các đăng ký thuộc đề tài mình hướng dẫn; thông tin nhóm, thành viên, đề tài, thời gian và trạng thái; nút Chấp thuận/Từ chối có modal và trường lý do bắt buộc khi từ chối.
- Nếu SV không phải nhóm trưởng, giải thích rõ vì sao không thực hiện được đăng ký.
- Không hiển thị sinh viên đã thuộc nhóm khác trong danh sách lựa chọn.

### 9.6. Báo cáo – `/reports` và form nộp báo cáo

- Danh sách có lọc theo đợt/đề tài/nhóm/trạng thái; cột nhóm, đề tài, tiêu đề, phiên bản, người nộp, ngày nộp, trạng thái và thao tác.
- Nhóm trưởng thấy nút “Nộp báo cáo” khi nhóm đã được chấp thuận; người khác không thấy hoặc không thể thao tác và phải được backend chặn.
- Form gồm nhóm (chỉ nhóm của người dùng), tiêu đề, file upload, mô tả; nêu định dạng và giới hạn 20 MB; hiện phiên bản sẽ được tạo khi nộp.
- Tải/xem báo cáo chỉ dành cho người có quyền tương ứng; thao tác xóa/thu hồi nếu nghiệp vụ cho phép phải có xác nhận và lưu lịch sử.

### 9.7. Hội đồng – `/councils`, `/councils/new`, `/councils/{id}`

**Danh sách:** tên/mã hội đồng, đợt, số thành viên `x/5`, chủ tịch, thư ký, số đề tài, tiến độ chấm, trạng thái. Dùng cảnh báo trực quan nếu ít hơn 3/thừa 5/thiếu chức danh bắt buộc.

**Form:** đợt, mã, tên, ngày báo cáo (theo quy tắc đợt), chọn thành viên và gán vai trò; chọn đề tài đã nộp báo cáo. Ghi chú quy tắc 3–5 GV, đúng một chủ tịch và một thư ký.

**Trang chi tiết:** hai vùng thông tin hội đồng và thành viên/đề tài; thao tác thêm/bớt thành viên với xác nhận; bảng kiểm “Hội đồng hợp lệ?” hiển thị từng điều kiện và dấu đạt/không đạt; nút “Hoàn tất hội đồng” chỉ khả dụng khi hợp lệ.

### 9.8. Phân công chấm – `/review-assignments`

- Danh sách lọc theo hội đồng/đề tài/giảng viên/trạng thái; hiển thị hội đồng, đề tài, người chấm, trạng thái.
- Form tạo phân công: hội đồng, đề tài thuộc hội đồng, giảng viên chấm là thành viên hội đồng.
- Hiển thị cảnh báo: “Giảng viên hướng dẫn không được chấm đề tài mình hướng dẫn”. Backend phải chặn cả khi GVHD thứ hai hoặc người tạo đề tài bị chọn trái quy tắc.
- Có tiến độ số người đã chấm/tổng số người được giao.

### 9.9. Nhập điểm và tổng hợp – `/evaluations`

- **Giảng viên:** hai nhóm “Cần chấm” và “Đã chấm”; mỗi dòng có đề tài, hội đồng, hạn chót, số ngày còn lại và trạng thái.
- **Chủ tịch hội đồng:** nhóm đề tài chưa phân công, nhóm đủ điểm chờ tổng hợp, bảng tổng hợp và nhóm kết quả chờ công bố (nếu có quyền xem).
- Form chấm có điểm 0–10, tối đa 2 số thập phân, nhận xét bắt buộc, thời hạn và nút Lưu/Gửi điểm.
- Bảng tổng hợp hiển thị điểm của từng người chấm, nhận xét, điểm trung bình làm tròn 2 chữ số và kết quả Đạt/Không đạt.
- Nút “Tổng hợp điểm” chỉ hiện/hoạt động khi tất cả phân công đã chấm xong. Nút công bố phải có modal xác nhận và chỉ người có `RESULT_PUBLISH` được thao tác.
- Sau khi công bố, hiện trạng thái khóa và không cho chỉnh sửa điểm/kết quả.

### 9.10. Kết quả sinh viên – `/results`

- Hiển thị kết quả đã công bố của nhóm sinh viên đăng nhập.
- Mỗi card có tên nhóm, đề tài, điểm cuối nổi bật, badge Đạt/Không đạt, hội đồng, ngày công bố và nhận xét tổng hợp nếu có.
- Không hiển thị kết quả chưa công bố hoặc kết quả của nhóm khác.
- Empty state: “Chưa có kết quả được công bố cho nhóm của bạn”.

### 9.11. Thông báo – `/notifications`

- Danh sách card/bảng gồm tiêu đề, trích nội dung, loại Nhà trường/Khoa, ngày đăng, trạng thái (với người quản lý), đối tượng nhận.
- Người dùng thường chỉ thấy thông báo đã công bố phù hợp với vai trò của mình.
- Admin/Trưởng khoa tạo/sửa bản nháp, chọn đối tượng nhận, công bố/thu hồi. Đã công bố không xóa hẳn; thao tác thu hồi cần xác nhận.
- Nội dung dài xem được trên trang chi tiết; escape an toàn, không render HTML tùy ý người dùng.

### 9.12. Tài khoản – `/users`, form và hồ sơ cá nhân

- Bảng tìm kiếm tài khoản theo username/họ tên/email; lọc vai trò/trạng thái; phân trang.
- Hiển thị tài khoản, họ tên, email, vai trò, trạng thái, ngày tạo và các thao tác phù hợp quyền.
- Form tài khoản chia khối: Thông tin đăng nhập; thông tin cá nhân; vai trò; hồ sơ SV/GV nếu cần.
- Khóa/mở khóa có modal xác nhận; không cho tự khóa Admin hiện tại hoặc loại bỏ Admin cuối cùng.
- Hồ sơ cá nhân: card thông tin và card đổi mật khẩu tách bạch; validation rõ, không hiển thị hash mật khẩu.

### 9.13. Vai trò và phân quyền – `/roles`, `/permissions`

- `/roles`: danh sách, tạo/sửa, xem chi tiết. Trang chi tiết hiển thị các quyền theo nhóm với checkbox có label. Vai trò hệ thống không có nút xóa.
- `/permissions`: danh mục quyền, mã quyền hiển thị monospace, tên và mô tả tiếng Việt; form tạo/sửa/chi tiết nếu nghiệp vụ cho phép.
- Mọi thao tác thay đổi quyền phải được bảo vệ bằng `ROLE_MANAGE`/`PERMISSION_MANAGE`, có kiểm tra ràng buộc và ghi `audit_logs`.

### 9.14. Trang lỗi

- 403: “Bạn không có quyền thực hiện thao tác này”.
- 404: “Không tìm thấy trang hoặc dữ liệu yêu cầu”.
- 500: “Đã xảy ra lỗi. Vui lòng thử lại hoặc liên hệ quản trị viên”.
- Có nút quay lại/tới Tổng quan phù hợp; không in stack trace, SQL, đường dẫn file hoặc thông tin nhạy cảm.

---

## 10. JAVASCRIPT THUẦN VÀ TƯƠNG TÁC

Tệp đề xuất: `static/js/app.js`. Dùng JavaScript thuần, không thêm dependency. Script phải kiểm tra phần tử tồn tại trước khi thao tác, để không gây lỗi trên những trang không dùng chức năng đó.

1. Sidebar/drawer trên mobile: mở/đóng bằng nút menu, đóng khi bấm ra ngoài hoặc nhấn Esc.
2. Modal xác nhận bằng `<dialog>` cho xóa, từ chối, khóa tài khoản, công bố/thu hồi kết quả và hoàn tất hội đồng. Không dùng `window.confirm`.
3. Flash message: đóng được; tự ẩn success/info sau khoảng 5 giây; không tự ẩn error.
4. Ẩn/hiện `review_deadline` và `council_report_date` theo loại đợt; xóa giá trị khi trường bị ẩn. Backend vẫn xác thực.
5. Tìm kiếm nhanh danh sách SV khi lập nhóm.
6. Chống gửi form nhiều lần bằng vô hiệu hóa nút submit trong lúc gửi; không vô hiệu hóa các nút điều hướng không liên quan.
7. Hỗ trợ mở rộng/thu gọn mô tả đề tài, thao tác cần thiết khác theo trang.
8. Nếu dùng lọc/phân trang phía trình duyệt cho một bảng nhỏ, hãy ghi rõ đây chỉ là lọc giao diện; ưu tiên phân trang/query tại backend cho dữ liệu lớn. Không tải hàng nghìn bản ghi chỉ để lọc bằng JS.
9. Không phụ thuộc `sessionStorage` để đảm bảo tính đúng đắn của nghiệp vụ; nếu dùng thì phải có `try/catch` và coi là tùy chọn UX.

---

## 11. KIỂM THỬ BẮT BUỘC

Mỗi quy tắc nghiệp vụ trọng yếu phải có tối thiểu một ca hợp lệ và một ca bị từ chối. Dùng JUnit/Spring Boot Test/Mockito và `spring-security-test` khi phù hợp; kiểm tra database integration trên môi trường test phù hợp. Không được chỉ test happy path.

### 11.1. Đợt đăng ký và đề tài

- Thời gian sai thứ tự bị từ chối; đợt không cho thao tác ngoài khoảng cho phép.
- Không chấp nhận hạn chót GVPB sai loại; không chấp nhận ngày báo cáo hội đồng ở loại khác KLTN.
- Đề tài có 1 hoặc 2 GVHD hợp lệ; 0 hoặc 3 GVHD, GVHD trùng, mã đề tài trùng phải bị từ chối.
- Giảng viên không được đề xuất đề tài thuộc bộ môn khác; chỉ Trưởng khoa/Admin có quyền duyệt.
- Sinh viên không đăng ký được đề tài chưa được duyệt/công bố hoặc khác đợt.

### 11.2. Nhóm, đăng ký và báo cáo

- Nhóm 1–3 SV, đúng một trưởng nhóm; nhóm 4 người, không có trưởng nhóm hoặc nhiều trưởng nhóm bị từ chối.
- SV đã thuộc nhóm khác (kể cả nhóm trưởng) không thể tham gia nhóm thứ hai; test tình huống hai request đồng thời.
- Chỉ nhóm trưởng đăng ký đề tài và nộp báo cáo.
- Một nhóm không thể có hai đăng ký hoạt động; một đề tài không thể được chấp thuận cho hai nhóm.
- Khi chấp thuận đề tài, đồng bộ `topic_registrations`, `student_groups.topic_id`, trạng thái nhóm và từ chối các đăng ký chờ khác trong một transaction.
- Chỉ GVHD của đề tài được chấp thuận/từ chối; từ chối phải có lý do.
- Tệp sai định dạng/quá 20 MB bị từ chối; file không được tải bởi người ngoài phạm vi quyền; phiên bản nhóm không trùng.

### 11.3. Hội đồng, phân công, chấm điểm và kết quả

- Hội đồng thiếu/thừa thành viên, thiếu chủ tịch/thư ký hoặc trùng GV bị từ chối chuyển `READY`.
- Người chấm phải thuộc hội đồng; đề tài và hội đồng phải cùng đợt; người chấm không phải GVHD (kiểm tra cả GVHD thứ hai) và đáp ứng quy tắc người tạo đề tài.
- Người không phải chủ tịch của hội đồng đó không được quản lý phân công/tổng hợp.
- Điểm ngoài 0–10, nhận xét trống, chấm trùng hoặc nhập điểm quá hạn bị từ chối (ngoại lệ Admin cần audit log nếu áp dụng).
- Thiếu một điểm thành phần thì không thể tổng hợp.
- Ví dụ test: các điểm 8,00; 7,50; 9,00 phải cho trung bình làm tròn thành 8,17.
- Sau công bố không thể thay đổi điểm/kết quả; SV không thể xem kết quả chưa công bố hoặc của nhóm khác.

### 11.4. Bảo mật và toàn vẹn schema

- Người chưa đăng nhập được chuyển về `/login` khi truy cập trang bảo vệ.
- Mỗi role chỉ gọi được endpoint được phép; quyền sở hữu được kiểm tra.
- POST thiếu CSRF bị từ chối; tài khoản bị khóa không đăng nhập được; cơ chế khóa tạm sau nhiều lần sai hoạt động.
- Khởi tạo schema, ánh xạ entity và chạy ứng dụng không gặp lỗi thiếu cột/NOT NULL/constraint.
- Mọi thao tác trọng yếu có audit log không chứa secret.

### 11.5. Kiểm thử UI

- Kiểm tra các vai trò thực tế trên màn hình dashboard, menu, danh sách và form.
- Chrome DevTools tại 1366 px, 768 px và 375 px: không tràn/cuộn ngang toàn trang; không có lỗi Console; không có request 404; form và modal hoạt động.
- Chạy Lighthouse Accessibility nếu môi trường hỗ trợ; ghi điểm và vấn đề còn lại, không bịa điểm nếu chưa chạy.
- Kiểm tra bàn phím, focus, label, lỗi validation, trạng thái rỗng, xác nhận thao tác nguy hiểm và badge tiếng Việt.

---

## 12. KẾ HOẠCH TRIỂN KHAI THEO GIAI ĐOẠN

**Giai đoạn 0 – Khảo sát dự án**

- Đọc mã nguồn và docs; xác minh Java/dependency/config; chạy test nền; lập bảng hiện trạng; liệt kê khác biệt so với đề bài.
- **Kết quả:** báo cáo khảo sát, kế hoạch sửa và danh sách rủi ro. Dừng chờ tôi duyệt nếu có blocker hoặc cấu hình Java không đúng 21.

**Giai đoạn 1 – Nền tảng, schema, bảo mật và layout chung**

- Thiết kế/cập nhật `HT_QLDT.sql`; chỉnh entity/repository nền; biến môi trường; cấu hình security; audit log; dữ liệu demo riêng; trang đăng nhập, layout/fragment, sidebar, CSS dùng chung, JS nền và trang lỗi.
- **Kiểm tra:** schema chạy trên MySQL mới, JPA validate thành công, ứng dụng khởi động, login/logout hoạt động, khung chung hiển thị.

**Giai đoạn 2 – Tài khoản, vai trò và phân quyền**

- CRUD tài khoản, hồ sơ cá nhân, đổi mật khẩu, vai trò/quyền, khóa/mở khóa, khởi tạo quyền, kiểm tra authority và quyền Admin cuối cùng.
- **Kiểm tra:** test theo từng role, CSRF, tài khoản khóa và audit log.

**Giai đoạn 3 – Đợt đăng ký**

- Danh sách, chi tiết, tạo/sửa, công bố/đóng; kiểm tra thời gian và trường theo loại; hoàn thiện form responsive.
- **Kiểm tra:** validation, trạng thái, thời gian ngoài khung và test loại đợt.

**Giai đoạn 4 – Đề tài**

- GV đề xuất/sửa/gửi duyệt lại, Trưởng khoa/Admin duyệt/từ chối, GVHD 1–2 người, công bố danh sách và lọc theo bộ môn/đợt.
- **Kiểm tra:** quyền, ràng buộc GVHD/mã đề tài, quy trình duyệt và giao diện.

**Giai đoạn 5 – Nhóm và đăng ký đề tài**

- Lập nhóm, chọn thành viên, thêm/bớt, đăng ký/hủy, GVHD chấp thuận/từ chối và cập nhật dữ liệu đồng bộ.
- **Kiểm tra:** tối đa 3 người, một nhóm/SV, một đề tài hoạt động/nhóm, một nhóm được duyệt/đề tài, race conditions.

**Giai đoạn 6 – Báo cáo**

- Upload, versioning, kiểm tra dung lượng/định dạng, lưu an toàn, xem/tải theo quyền; hoàn thiện danh sách và form.
- **Kiểm tra:** nhóm trưởng, trạng thái nhóm, định dạng, giới hạn dung lượng, quyền tải.

**Giai đoạn 7 – Hội đồng phản biện**

- CRUD hội đồng, chọn đề tài, quản lý thành viên, chủ tịch/thư ký, điều kiện `READY` và trang chi tiết kiểm tra hợp lệ.
- **Kiểm tra:** 3–5 GV, đúng chức danh, không trùng, đúng đợt.

**Giai đoạn 8 – Phân công và nhập điểm**

- Phân công người chấm, danh sách việc cần chấm, form điểm, hạn chót, nhận xét, khóa nhập sau deadline theo quy tắc.
- **Kiểm tra:** người chấm đúng hội đồng, không phải GVHD, điểm hợp lệ, không chấm trùng.

**Giai đoạn 9 – Tổng hợp và công bố kết quả**

- Trung bình cộng/rounding, ngưỡng đạt, xem điểm thành phần trước khi chốt, công bố và trang kết quả sinh viên.
- **Kiểm tra:** đủ điểm, tính đúng, khóa sau công bố, quyền xem của sinh viên.

**Giai đoạn 10 – Thông báo chính thức**

- Soạn, lưu nháp, phân nhóm nhận, công bố/thu hồi, danh sách/chi tiết và thông báo mới trên dashboard.
- **Kiểm tra:** lọc theo role, chỉ thấy thông báo được công bố.

**Giai đoạn 11 – Hoàn thiện dashboard và UI/UX**

- Hoàn thiện từng trang theo mục 9; rà soát màu, badge, table, form, modal, empty state, responsive và truy cập bàn phím; loại bỏ CSS trùng sau khi xác minh không còn dùng.
- **Kiểm tra:** tất cả trang chung layout; menu không có đường cụt; DevTools 3 kích thước; không có lỗi console/404.

**Giai đoạn 12 – Rà soát, tài liệu và nghiệm thu tổng thể**

- Chạy toàn bộ test/build, luồng E2E thủ công, kiểm tra schema/entity, rà bảo mật, cập nhật README và docs, ERD theo schema thực tế, Postman collection cho REST API tồn tại.
- **Kiểm tra:** checklist nghiệm thu tại mục 13. Báo cáo việc đạt/chưa đạt trung thực.

**Quy trình bên trong mỗi giai đoạn:** hiểu code → cập nhật test → triển khai → chạy test/build → kiểm tra màn hình/luồng liên quan → sửa lỗi phát sinh → báo cáo. Sau báo cáo, chờ tôi gửi “TIẾP TỤC”. Không tự nhảy qua giai đoạn khi test lỗi.

---

## 13. TIÊU CHÍ NGHIỆM THU CUỐI CÙNG

Chỉ đánh dấu đạt khi có bằng chứng thực tế:

1. Công nghệ đúng cấu hình dự án; Java 21 được xác minh; không thêm framework/dependency trái phép.
2. `HT_QLDT.sql` chạy được trên MySQL mới; Workbench Reverse Engineer được EER; entity/JPA khớp schema; không lỗi thiếu cột hoặc constraint.
3. Đi được luồng hoàn chỉnh từ tạo đợt đến công bố kết quả và sinh viên xem kết quả.
4. Đầy đủ chức năng tài khoản, vai trò, quyền, nhóm, đăng ký, báo cáo, hội đồng, phân công, chấm điểm, tổng hợp, công bố, thông báo và dashboard.
5. Mọi quy tắc nghiệp vụ cốt lõi được kiểm tra ở backend và có test cả ca đúng/sai.
6. Không có endpoint ghi dữ liệu thiếu kiểm tra authority/quyền sở hữu; CSRF bật; tài khoản khóa bị chặn; không lộ dữ liệu hay file cho người không có quyền.
7. Mọi trang sau đăng nhập dùng layout chung; menu theo quyền; biểu mẫu có label; lỗi gần trường; thao tác nguy hiểm có xác nhận; trạng thái hiển thị bằng tiếng Việt.
8. Responsive tại 1366/768/375 px, không cuộn ngang toàn trang ở mobile; Console sạch, không có request 404 khi kiểm tra các luồng; ghi kết quả Lighthouse nếu đã chạy.
9. `./mvnw test` thành công và `./mvnw -DskipTests package` thành công. Nếu không thể chạy do thiếu MySQL/JDK/cấu hình, nêu rõ blocker và lệnh chưa chạy, không tuyên bố pass.
10. Có README tiếng Việt: JDK/MySQL cần thiết, biến môi trường, cách nạp schema, dữ liệu demo, cách chạy ứng dụng/test và tài liệu liên quan.
11. Không để dữ liệu demo/mật khẩu thật/secret trong repo ngoài quy ước demo an toàn.
12. Không commit/push nếu tôi chưa yêu cầu.

---

## 14. DỮ LIỆU DEMO VÀ TÀI LIỆU BÀN GIAO

Tạo dữ liệu demo ở profile riêng hoặc script riêng, không lẫn vào schema chuẩn. Tối thiểu có:

- 1 Admin; 1 Trưởng khoa; 2 tài khoản Chủ tịch hội đồng; ít nhất 6 giảng viên thuộc ít nhất 3 bộ môn; ít nhất 12 sinh viên.
- Ít nhất 1 đợt TLCN và 1 đợt KLTN ở các giai đoạn khác nhau.
- Đề tài đủ trạng thái chờ duyệt/đã duyệt/từ chối/đã công bố; nhóm, đăng ký, báo cáo, hội đồng, phân công, điểm và kết quả đủ để kiểm thử giao diện.
- Thông báo loại Nhà trường và Khoa, có bản nháp/đã công bố.
- Mật khẩu demo phải ghi rõ chỉ dùng môi trường demo; không dùng thông tin đăng nhập thật.

README và `docs/` tối thiểu gồm:

- Sơ đồ kiến trúc và luồng nghiệp vụ.
- ERD đúng với schema thực tế.
- Bảng vai trò/quyền.
- Hướng dẫn cấu hình JDK 21, MySQL và biến môi trường.
- Hướng dẫn chạy `HT_QLDT.sql`, bật profile demo, chạy app/test.
- Hướng dẫn upload file, thư mục lưu trữ và quyền tải.
- Kết quả kiểm thử, lỗi còn lại, các giả định cần tôi xác nhận.
- Postman collection chỉ cho các endpoint REST thực sự có trong dự án.

---

## 15. BÁO CÁO BẮT BUỘC SAU MỖI GIAI ĐOẠN

Báo cáo ngắn gọn bằng tiếng Việt theo mẫu:

1. **Kết quả:** đã hoàn thành mục nào, chưa hoàn thành mục nào.
2. **Tệp đã thêm/sửa/xóa:** liệt kê các tệp quan trọng và mục đích.
3. **Kiểm thử:** lệnh đã chạy, số test pass/fail, kết quả build; không ghi “đã kiểm tra” nếu chưa làm.
4. **UI:** các màn hình đã xem, kích thước đã kiểm tra, lỗi Console/Network, khả năng truy cập nếu đã đo.
5. **Vấn đề phát hiện:** bug nghiệp vụ, chênh lệch schema/entity, vấn đề bảo mật/hiệu năng; nêu mức độ và phương án.
6. **Giả định/cần xác nhận:** ghi các điểm chưa rõ, quyết định tạm thời và ảnh hưởng.
7. **Bước tiếp theo:** mô tả ngắn gọn giai đoạn tiếp theo nhưng chưa bắt đầu cho đến khi tôi đồng ý.

**Bắt đầu ngay từ Giai đoạn 0:** khảo sát repository, xác minh công nghệ và tình trạng test hiện tại. Không viết hàng loạt code trước khi hoàn tất khảo sát. Sau khi báo cáo Giai đoạn 0, hãy dừng và chờ tôi xác nhận.
