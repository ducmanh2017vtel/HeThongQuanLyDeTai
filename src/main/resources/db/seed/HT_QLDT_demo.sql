-- ============================================================================
--  HT_QLDT_demo.sql  -  Dữ liệu mẫu cho profile=demo
--  CHẠY SAU HT_QLDT.sql.
--  Tất cả mật khẩu tài khoản demo đều là "Demo@123" (đã BCrypt-hash sẵn).
--  Mật khẩu chỉ dùng cho môi trường DEMO, KHÔNG dùng cho production.
-- ============================================================================

USE hethong_quanlydetai;

-- BCrypt hash của "Demo@123" (cost 10)
SET @pw := '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624v8.';

-- Bộ môn (3 bộ môn)
INSERT INTO departments (code, name) VALUES
    ('CNPM',  'Công nghệ phần mềm'),
    ('KHMT',  'Khoa học máy tính'),
    ('MMT',   'Mạng máy tính và Truyền thông');

-- Tài khoản quản trị + trưởng khoa + chủ tịch HĐ
-- (user_id ở đây là giá trị gợi ý; INSERT tuần tự nên thứ tự dưới đây tạo ra id liên tiếp)
INSERT INTO users (username, password, full_name, email, phone, status) VALUES
    ('admin',        @pw, 'Quản trị hệ thống',    'admin@cntt.edu.vn',     NULL, 'ACTIVE'),
    ('truongkhoa',   @pw, 'Trần Trưởng Khoa',     'truongkhoa@cntt.edu.vn', NULL, 'ACTIVE'),
    ('cthd1',        @pw, 'Nguyễn Chủ Tịch A',    'cthd1@cntt.edu.vn',      NULL, 'ACTIVE'),
    ('cthd2',        @pw, 'Phạm Chủ Tịch B',      'cthd2@cntt.edu.vn',      NULL, 'ACTIVE');

-- Gán vai trò
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE (u.username='admin'      AND r.code='ADMIN')
   OR (u.username='truongkhoa' AND r.code='TRUONG_KHOA')
   OR (u.username='cthd1'      AND r.code='CHU_TICH_HOI_DONG')
   OR (u.username='cthd2'      AND r.code='CHU_TICH_HOI_DONG');

-- GV demo (6 người, 3 bộ môn)
INSERT INTO users (username, password, full_name, email, status) VALUES
    ('gv_cnpm1', @pw, 'Lê Hoàng CNPM',   'gv.cnpm1@cntt.edu.vn', 'ACTIVE'),
    ('gv_cnpm2', @pw, 'Võ Thị Phần Mềm', 'gv.cnpm2@cntt.edu.vn', 'ACTIVE'),
    ('gv_khmt1', @pw, 'Đặng Văn KHMT',   'gv.khmt1@cntt.edu.vn', 'ACTIVE'),
    ('gv_khmt2', @pw, 'Bùi Thuật Toán',  'gv.khmt2@cntt.edu.vn', 'ACTIVE'),
    ('gv_mmt1',  @pw, 'Hoàng Mạng A',    'gv.mmt1@cntt.edu.vn',  'ACTIVE'),
    ('gv_mmt2',  @pw, 'Trịnh Mạng B',    'gv.mmt2@cntt.edu.vn',  'ACTIVE');

-- Gán vai trò GIANG_VIEN
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username LIKE 'gv\_%' AND r.code='GIANG_VIEN';

-- Hồ sơ giảng viên (mã, bộ môn)
INSERT INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(SUBSTRING(u.username,4,10)), 'ThS',
       (SELECT id FROM departments WHERE code='CNPM')
FROM users u WHERE u.username='gv_cnpm1';

INSERT INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(SUBSTRING(u.username,4,10)), 'TS',
       (SELECT id FROM departments WHERE code='CNPM')
FROM users u WHERE u.username='gv_cnpm2';

INSERT INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(SUBSTRING(u.username,4,10)), 'ThS',
       (SELECT id FROM departments WHERE code='KHMT')
FROM users u WHERE u.username='gv_khmt1';

INSERT INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(SUBSTRING(u.username,4,10)), 'TS',
       (SELECT id FROM departments WHERE code='KHMT')
FROM users u WHERE u.username='gv_khmt2';

INSERT INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(SUBSTRING(u.username,4,10)), 'ThS',
       (SELECT id FROM departments WHERE code='MMT')
FROM users u WHERE u.username='gv_mmt1';

INSERT INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(SUBSTRING(u.username,4,10)), 'ThS',
       (SELECT id FROM departments WHERE code='MMT')
FROM users u WHERE u.username='gv_mmt2';

-- 12 SV
INSERT INTO users (username, password, full_name, email, status) VALUES
    ('sv01','sv01@sv','Nguyễn Văn A','sv01@sv.edu.vn','ACTIVE'),
    ('sv02','sv02@sv','Nguyễn Văn B','sv02@sv.edu.vn','ACTIVE'),
    ('sv03','sv03@sv','Nguyễn Văn C','sv03@sv.edu.vn','ACTIVE'),
    ('sv04','sv04@sv','Trần Thị D','sv04@sv.edu.vn','ACTIVE'),
    ('sv05','sv05@sv','Trần Thị E','sv05@sv.edu.vn','ACTIVE'),
    ('sv06','sv06@sv','Trần Văn F','sv06@sv.edu.vn','ACTIVE'),
    ('sv07','sv07@sv','Lê Văn G','sv07@sv.edu.vn','ACTIVE'),
    ('sv08','sv08@sv','Lê Thị H','sv08@sv.edu.vn','ACTIVE'),
    ('sv09','sv09@sv','Phạm Văn I','sv09@sv.edu.vn','ACTIVE'),
    ('sv10','sv10@sv','Phạm Thị K','sv10@sv.edu.vn','ACTIVE'),
    ('sv11','sv11@sv','Hoàng Văn L','sv11@sv.edu.vn','ACTIVE'),
    ('sv12','sv12@sv','Hoàng Thị M','sv12@sv.edu.vn','ACTIVE');
-- Lưu ý: 2 cột trên (cột password và full_name) chỉ là placeholder cho khớp số cột;
-- sẽ được thay bằng @pw phía dưới.

UPDATE users SET password = @pw WHERE username LIKE 'sv%';
UPDATE users SET full_name = CONCAT('Sinh viên ', SUBSTRING(username,3,2)) WHERE username LIKE 'sv%';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username LIKE 'sv%' AND r.code='SINH_VIEN';

-- Hồ sơ sinh viên
INSERT INTO students (user_id, student_code, class_name, major)
SELECT u.id, CONCAT('SV',SUBSTRING(u.username,3,2)), 'CNTT-K15', 'Công nghệ thông tin'
FROM users u WHERE u.username LIKE 'sv%';

-- Hai đợt đăng ký (một TLCN đang ở giai đoạn SV, một KLTN ở giai đoạn GV)
INSERT INTO registration_periods
    (name, period_type, lecturer_start_at, lecturer_end_at,
     student_start_at, student_end_at, review_deadline, council_report_date,
     status, created_by)
VALUES
    ('Đợt TLCN học kỳ 2026-1', 'TLCN',
     '2026-01-15 00:00:00','2026-02-15 23:59:59',
     '2026-02-16 00:00:00','2026-03-20 23:59:59',
     '2026-04-30 23:59:59', NULL,
     'PUBLISHED',
     (SELECT id FROM users WHERE username='truongkhoa')),
    ('Đợt KLTN năm 2026', 'KLTN',
     '2026-09-01 00:00:00','2026-09-20 23:59:59',
     '2026-09-21 00:00:00','2026-10-15 23:59:59',
     '2026-10-25 23:59:59','2026-11-05 09:00:00',
     'PUBLISHED',
     (SELECT id FROM users WHERE username='truongkhoa'));

-- Vài đề tài minh hoạ (PENDING/APPROVED, có và chưa có GVHD thứ hai)
INSERT INTO topics
    (period_id, department_id, created_by_lecturer_id, topic_code, topic_name, description,
     status, published, approved_by, approved_at)
VALUES
    ((SELECT id FROM registration_periods WHERE name LIKE 'Đợt TLCN%'),
     (SELECT id FROM departments WHERE code='CNPM'),
     (SELECT l.id FROM lecturers l JOIN users u ON u.id=l.user_id WHERE u.username='gv_cnpm1'),
     'DT-TLCN-001', 'Xây dựng hệ thống quản lý đề tài sinh viên',
     'Phân tích và thiết kế hệ thống quản lý đề tài sinh viên khoa CNTT; bao gồm quản lý đợt, đề tài, nhóm và phản biện.',
     'APPROVED', 1,
     (SELECT id FROM users WHERE username='truongkhoa'),
     '2026-02-10 10:00:00'),

    ((SELECT id FROM registration_periods WHERE name LIKE 'Đợt TLCN%'),
     (SELECT id FROM departments WHERE code='KHMT'),
     (SELECT l.id FROM lecturers l JOIN users u ON u.id=l.user_id WHERE u.username='gv_khmt1'),
     'DT-TLCN-002', 'Nghiên cứu thuật toán tối ưu hóa cho bài toán lập lịch',
     'Khảo sát các thuật toán metaheuristic cho bài toán lập lịch; đề xuất cải tiến; đánh giá thực nghiệm trên bộ dữ liệu chuẩn.',
     'APPROVED', 1,
     (SELECT id FROM users WHERE username='truongkhoa'),
     '2026-02-10 10:05:00'),

    ((SELECT id FROM registration_periods WHERE name LIKE 'Đợt TLCN%'),
     (SELECT id FROM departments WHERE code='MMT'),
     (SELECT l.id FROM lecturers l JOIN users u ON u.id=l.user_id WHERE u.username='gv_mmt1'),
     'DT-TLCN-003', 'Xây dựng hệ thống giám sát mạng nội bộ',
     'Triển khai hệ thống giám sát mạng dựa trên các công cụ mã nguồn mở; phát hiện bất thường và cảnh báo tập trung.',
     'PENDING', 0, NULL, NULL),

    ((SELECT id FROM registration_periods WHERE name LIKE 'Đợt KLTN%'),
     (SELECT id FROM departments WHERE code='CNPM'),
     (SELECT l.id FROM lecturers l JOIN users u ON u.id=l.user_id WHERE u.username='gv_cnpm2'),
     'DT-KLTN-001', 'Nền tảng học trực tuyến cho khoa CNTT',
     'Thiết kế và triển khai nền tảng e-learning hỗ trợ quản lý khoá học, bài tập, chấm điểm tự động; đánh giá hiệu quả.',
     'APPROVED', 1,
     (SELECT id FROM users WHERE username='truongkhoa'),
     '2026-09-15 11:00:00');

-- GVHD cho các đề tài đã duyệt
INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
SELECT t.id, l.id, 1 FROM topics t JOIN lecturers l JOIN users u ON u.id=l.user_id
WHERE u.username='gv_cnpm1' AND t.topic_code='DT-TLCN-001';

INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
SELECT t.id, l.id, 2 FROM topics t JOIN lecturers l JOIN users u ON u.id=l.user_id
WHERE u.username='gv_cnpm2' AND t.topic_code='DT-TLCN-001';

INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
SELECT t.id, l.id, 1 FROM topics t JOIN lecturers l JOIN users u ON u.id=l.user_id
WHERE u.username='gv_khmt1' AND t.topic_code='DT-TLCN-002';

INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
SELECT t.id, l.id, 1 FROM topics t JOIN lecturers l JOIN users u ON u.id=l.user_id
WHERE u.username='gv_mmt1'  AND t.topic_code='DT-TLCN-003';

INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
SELECT t.id, l.id, 1 FROM topics t JOIN lecturers l JOIN users u ON u.id=l.user_id
WHERE u.username='gv_cnpm2' AND t.topic_code='DT-KLTN-001';

INSERT INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
SELECT t.id, l.id, 2 FROM topics t JOIN lecturers l JOIN users u ON u.id=l.user_id
WHERE u.username='gv_khmt2' AND t.topic_code='DT-KLTN-001';

-- Một nhóm SV mẫu ở trạng thái REGISTERED (đã đăng ký, chờ GVHD duyệt)
INSERT INTO student_groups (period_id, group_code, group_name, status)
VALUES ((SELECT id FROM registration_periods WHERE name LIKE 'Đợt TLCN%'),
        'N01', 'Nhóm 1 - CNPM', 'REGISTERED');

INSERT INTO topic_registrations (group_id, topic_id, status)
SELECT g.id, t.id, 'PENDING'
FROM student_groups g, topics t
WHERE g.group_code='N01' AND t.topic_code='DT-TLCN-001';

INSERT INTO group_members (group_id, student_id, is_leader)
SELECT g.id, s.id, 1 FROM student_groups g, students s JOIN users u ON u.id=s.user_id
WHERE g.group_code='N01' AND u.username='sv01';

INSERT INTO group_members (group_id, student_id, is_leader)
SELECT g.id, s.id, 0 FROM student_groups g, students s JOIN users u ON u.id=s.user_id
WHERE g.group_code='N01' AND u.username='sv02';

INSERT INTO group_members (group_id, student_id, is_leader)
SELECT g.id, s.id, 0 FROM student_groups g, students s JOIN users u ON u.id=s.user_id
WHERE g.group_code='N01' AND u.username='sv03';

-- Thông báo mẫu (Trường/Khoa)
INSERT INTO notifications (title, content, type, created_by, status, published_at)
VALUES
    ('Thông báo lịch bảo vệ KLTN 2026',
     'Lịch bảo vệ các đề tài KLTN năm 2026 sẽ được công bố vào tuần thứ 2 của tháng 11.',
     'FACULTY', (SELECT id FROM users WHERE username='truongkhoa'),
     'PUBLISHED', '2026-10-01 09:00:00'),
    ('Nộp báo cáo giữa kỳ TLCN',
     'Sinh viên làm tiểu luận chuyên ngành nộp báo cáo giữa kỳ trước ngày 30/04/2026.',
     'FACULTY', (SELECT id FROM users WHERE username='truongkhoa'),
     'PUBLISHED', '2026-03-01 09:00:00');

-- Thông báo cho tất cả vai trò
INSERT INTO notification_target_roles (notification_id, role_id)
SELECT n.id, r.id FROM notifications n CROSS JOIN roles r WHERE n.title LIKE 'Thông báo lịch%';

INSERT INTO notification_target_roles (notification_id, role_id)
SELECT n.id, r.id FROM notifications n CROSS JOIN roles r WHERE n.title LIKE 'Nộp báo cáo%';

-- ============================================================================
--  HẾT. Tài khoản demo: xem README.md mục "Tài khoản mẫu".
-- ============================================================================