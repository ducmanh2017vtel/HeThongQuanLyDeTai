-- ============================================================================
--  HT_QLDT_full.sql  -  Seed dữ liệu đầy đủ cho môi trường MySQL
--  Có thể chạy nhiều lần (idempotent). Dùng INSERT IGNORE / ON DUPLICATE KEY.
--
--  Dữ liệu:
--    - 1 admin, 1 truongkhoa, 2 cthd, 30 GV (10/bộ môn), 200 SV
--    - 3 bộ môn (CNPM/KHMT/MMT)
--    - 10 đợt đăng ký (TLCN + KLTN + MON_HOC + NCKH, nhiều năm)
--    - 80 đề tài (PENDING/APPROVED/REJECTED, đa dạng bộ môn)
--    - 25 nhóm SV + 25 topic_registrations + 50 group_members
--    - 5 hội đồng + 15 council_members + 5 council_topics + 10 review_assignments
--    - 10 evaluations + 5 review_results (PASSED/FAILED) + 1 đã published
--    - 10 thông báo mẫu
--
--  Mật khẩu chung: Demo@123 (BCrypt hash)
-- ============================================================================

USE hethong_quanlydetai;

SET @pw := '{bcrypt}$2a$10$GAi4gwGBkwfni5NR6Z66Ee2NZyyhdzhg0ZMWXHPT.83qIYQfUs9pq';
-- Ép collation cho phù hợp schema (utf8mb4_0900_ai_ci - MySQL 8 mặc định)
SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- ============================================================================
--  1.  Bộ môn (3)
-- ============================================================================
INSERT IGNORE INTO departments (code, name, status) VALUES
    ('CNPM', 'Công nghệ phần mềm',                            'ACTIVE'),
    ('KHMT', 'Khoa học máy tính',                              'ACTIVE'),
    ('MMT',  'Mạng máy tính và Truyền thông',                 'ACTIVE');

-- ============================================================================
--  2.  Tài khoản quản trị
-- ============================================================================
INSERT IGNORE INTO users (username, password, full_name, email, status, enabled) VALUES
    ('admin',      @pw, 'Quản trị hệ thống', 'admin@cntt.edu.vn',      'ACTIVE', 1),
    ('truongkhoa', @pw, 'Trần Trưởng Khoa',  'truongkhoa@cntt.edu.vn', 'ACTIVE', 1),
    ('cthd1',      @pw, 'Nguyễn Chủ Tịch A', 'cthd1@cntt.edu.vn',      'ACTIVE', 1),
    ('cthd2',      @pw, 'Phạm Chủ Tịch B',   'cthd2@cntt.edu.vn',      'ACTIVE', 1);

-- Vai trò cho admin
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE (u.username='admin'      AND r.code='ADMIN')
   OR (u.username='truongkhoa' AND r.code='TRUONG_KHOA')
   OR (u.username='cthd1'      AND r.code='CHU_TICH_HOI_DONG')
   OR (u.username='cthd2'      AND r.code='CHU_TICH_HOI_DONG');

-- ============================================================================
--  3.  Giảng viên (30 người, 10/bộ môn)
--      Mã: gv_cnpm_001..010, gv_khmt_001..010, gv_mmt_001..010
-- ============================================================================

-- CNPM (10)
INSERT IGNORE INTO users (username, password, full_name, email, status, enabled) VALUES
    ('gv_cnpm_001', @pw, 'Nguyễn Minh An',     'gv.cnpm.001@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_002', @pw, 'Trần Thị Bình',      'gv.cnpm.002@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_003', @pw, 'Lê Văn Cường',       'gv.cnpm.003@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_004', @pw, 'Phạm Thị Dung',      'gv.cnpm.004@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_005', @pw, 'Hoàng Văn Em',       'gv.cnpm.005@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_006', @pw, 'Vũ Thị Phượng',     'gv.cnpm.006@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_007', @pw, 'Đặng Văn Giang',     'gv.cnpm.007@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_008', @pw, 'Bùi Thị Hoa',        'gv.cnpm.008@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_009', @pw, 'Đỗ Văn Khánh',      'gv.cnpm.009@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_cnpm_010', @pw, 'Ngô Thị Lan',        'gv.cnpm.010@cntt.edu.vn', 'ACTIVE', 1);

-- KHMT (10)
INSERT IGNORE INTO users (username, password, full_name, email, status, enabled) VALUES
    ('gv_khmt_001', @pw, 'Trịnh Văn Minh',     'gv.khmt.001@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_002', @pw, 'Lý Thị Nga',         'gv.khmt.002@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_003', @pw, 'Đinh Văn Oanh',      'gv.khmt.003@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_004', @pw, 'Mai Thị Phương',     'gv.khmt.004@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_005', @pw, 'Tô Văn Quân',        'gv.khmt.005@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_006', @pw, 'Hồ Thị Rong',        'gv.khmt.006@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_007', @pw, 'Chu Văn Sơn',        'gv.khmt.007@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_008', @pw, 'Dương Thị Tâm',      'gv.khmt.008@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_009', @pw, 'Lưu Văn Uy',         'gv.khmt.009@cntt.edu.vn', 'ACTIVE', 1),
    ('gv_khmt_010', @pw, 'Phan Thị Vân',       'gv.khmt.010@cntt.edu.vn', 'ACTIVE', 1);

-- MMT (10)
INSERT IGNORE INTO users (username, password, full_name, email, status, enabled) VALUES
    ('gv_mmt_001', @pw, 'Võ Văn Xuân',        'gv.mmt.001@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_002', @pw, 'Hà Thị Yến',         'gv.mmt.002@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_003', @pw, 'Châu Văn Bảo',       'gv.mmt.003@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_004', @pw, 'Kim Thị Châu',       'gv.mmt.004@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_005', @pw, 'Lâm Văn Danh',       'gv.mmt.005@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_006', @pw, 'Ông Thị Hà',         'gv.mmt.006@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_007', @pw, 'Quách Văn Khải',     'gv.mmt.007@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_008', @pw, 'Tống Thị Linh',      'gv.mmt.008@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_009', @pw, 'Ứng Văn Mạnh',       'gv.mmt.009@cntt.edu.vn',  'ACTIVE', 1),
    ('gv_mmt_010', @pw, 'Xung Thị Ngọc',      'gv.mmt.010@cntt.edu.vn',  'ACTIVE', 1);

-- Vai trò GIANG_VIEN cho tất cả GV
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username LIKE 'gv\\_%' COLLATE utf8mb4_unicode_ci AND r.code='GIANG_VIEN';

-- Hồ sơ giảng viên
INSERT IGNORE INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(u.username), 'ThS',
       (SELECT id FROM departments WHERE code='CNPM')
FROM users u WHERE u.username LIKE 'gv\_cnpm\_%';

INSERT IGNORE INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(u.username), 'ThS',
       (SELECT id FROM departments WHERE code='KHMT')
FROM users u WHERE u.username LIKE 'gv\_khmt\_%';

INSERT IGNORE INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, UPPER(u.username), 'ThS',
       (SELECT id FROM departments WHERE code='MMT')
FROM users u WHERE u.username LIKE 'gv\_mmt\_%';

-- 1 số GV là TS để đa dạng
UPDATE lecturers SET academic_degree='TS'
WHERE lecturer_code IN ('GV_CNPM_002','GV_CNPM_005','GV_KHMT_003','GV_KHMT_007','GV_MMT_004');

-- ============================================================================
--  4.  Sinh viên (200 người)
--      Phân bố 8 lớp:
--        CNTT-K15 (25), CNTT-K16 (25), CNTT-K17 (25), CNTT-K18 (25)
--        KHMT-K15 (25), KHMT-K16 (25), KHMT-K17 (25)
--        MMT-K15 (25)
--      Tổng: 8 * 25 = 200
-- ============================================================================

-- Helper: tạo user SV
DROP TEMPORARY TABLE IF EXISTS tmp_sv_users;
CREATE TEMPORARY TABLE tmp_sv_users (
    username VARCHAR(60) PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(160) NOT NULL,
    class_name VARCHAR(60) NOT NULL,
    major VARCHAR(120) NOT NULL
);

-- CNTT-K15 (25 SV: sv_cntt15_001..025)
INSERT INTO tmp_sv_users (username, full_name, email, class_name, major)
SELECT
    CONCAT('sv_cntt15_', LPAD(seq, 3, '0')),
    CONCAT('Nguyễn Văn ', CHAR(64 + seq)),
    CONCAT('sv.cntt15.', LPAD(seq, 3, '0'), '@sv.cntt.edu.vn'),
    'CNTT-K15',
    'Công nghệ thông tin'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
      UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) AS s;

-- CNTT-K16
INSERT INTO tmp_sv_users (username, full_name, email, class_name, major)
SELECT
    CONCAT('sv_cntt16_', LPAD(seq, 3, '0')),
    CONCAT('Trần Thị ', CHAR(64 + seq)),
    CONCAT('sv.cntt16.', LPAD(seq, 3, '0'), '@sv.cntt.edu.vn'),
    'CNTT-K16',
    'Công nghệ thông tin'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
      UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) AS s;

-- CNTT-K17
INSERT INTO tmp_sv_users (username, full_name, email, class_name, major)
SELECT
    CONCAT('sv_cntt17_', LPAD(seq, 3, '0')),
    CONCAT('Lê Văn ', CHAR(64 + seq)),
    CONCAT('sv.cntt17.', LPAD(seq, 3, '0'), '@sv.cntt.edu.vn'),
    'CNTT-K17',
    'Công nghệ thông tin'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
      UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) AS s;

-- CNTT-K18
INSERT INTO tmp_sv_users (username, full_name, email, class_name, major)
SELECT
    CONCAT('sv_cntt18_', LPAD(seq, 3, '0')),
    CONCAT('Phạm Thị ', CHAR(64 + seq)),
    CONCAT('sv.cntt18.', LPAD(seq, 3, '0'), '@sv.cntt.edu.vn'),
    'CNTT-K18',
    'Công nghệ thông tin'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
      UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) AS s;

-- KHMT-K15
INSERT INTO tmp_sv_users (username, full_name, email, class_name, major)
SELECT
    CONCAT('sv_khmt15_', LPAD(seq, 3, '0')),
    CONCAT('Hoàng Văn ', CHAR(64 + seq)),
    CONCAT('sv.khmt15.', LPAD(seq, 3, '0'), '@sv.cntt.edu.vn'),
    'KHMT-K15',
    'Khoa học máy tính'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
      UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) AS s;

-- KHMT-K16
INSERT INTO tmp_sv_users (username, full_name, email, class_name, major)
SELECT
    CONCAT('sv_khmt16_', LPAD(seq, 3, '0')),
    CONCAT('Vũ Thị ', CHAR(64 + seq)),
    CONCAT('sv.khmt16.', LPAD(seq, 3, '0'), '@sv.cntt.edu.vn'),
    'KHMT-K16',
    'Khoa học máy tính'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
      UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) AS s;

-- KHMT-K17
INSERT INTO tmp_sv_users (username, full_name, email, class_name, major)
SELECT
    CONCAT('sv_khmt17_', LPAD(seq, 3, '0')),
    CONCAT('Đặng Văn ', CHAR(64 + seq)),
    CONCAT('sv.khmt17.', LPAD(seq, 3, '0'), '@sv.cntt.edu.vn'),
    'KHMT-K17',
    'Khoa học máy tính'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
      UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) AS s;

-- MMT-K15
INSERT INTO tmp_sv_users (username, full_name, email, class_name, major)
SELECT
    CONCAT('sv_mmt15_', LPAD(seq, 3, '0')),
    CONCAT('Bùi Văn ', CHAR(64 + seq)),
    CONCAT('sv.mmt15.', LPAD(seq, 3, '0'), '@sv.cntt.edu.vn'),
    'MMT-K15',
    'Mạng máy tính và Truyền thông'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
      UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
      UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15
      UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20
      UNION SELECT 21 UNION SELECT 22 UNION SELECT 23 UNION SELECT 24 UNION SELECT 25) AS s;

-- Insert users từ tmp
INSERT IGNORE INTO users (username, password, full_name, email, status, enabled)
SELECT username, @pw, full_name, email, 'ACTIVE', 1 FROM tmp_sv_users;

-- Vai trò SINH_VIEN
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.username LIKE 'sv\\_%' COLLATE utf8mb4_unicode_ci AND r.code='SINH_VIEN';

-- Hồ sơ sinh viên
INSERT IGNORE INTO students (user_id, student_code, class_name, major)
SELECT u.id,
       UPPER(u.username),
       t.class_name,
       t.major
FROM users u JOIN tmp_sv_users t ON u.username = t.username
WHERE u.username LIKE 'sv\\_%' COLLATE utf8mb4_unicode_ci;

DROP TEMPORARY TABLE tmp_sv_users;

-- ============================================================================
--  5.  10 đợt đăng ký
--      Trước 2026: 4 đợt (TLCN + KLTN nhiều năm/kỳ) - đã CLOSED
--      2026: 6 đợt (TLCN hk1, TLCN hk2, KLTN 2026, NCKH 2026, MON_HOC 2026-1, MON_HOC 2026-2)
-- ============================================================================

INSERT IGNORE INTO registration_periods
    (name, period_type, lecturer_start_at, lecturer_end_at,
     student_start_at, student_end_at, review_deadline, council_report_date,
     status, created_by)
VALUES
    -- Đã CLOSED
    ('Đợt TLCN học kỳ 2024-1', 'TLCN',
     '2024-01-15 00:00:00','2024-02-15 23:59:59',
     '2024-02-16 00:00:00','2024-03-20 23:59:59',
     '2024-04-30 23:59:59', NULL,
     'CLOSED', (SELECT id FROM users WHERE username='truongkhoa')),

    ('Đợt KLTN năm 2024', 'KLTN',
     '2024-09-01 00:00:00','2024-09-20 23:59:59',
     '2024-09-21 00:00:00','2024-10-15 23:59:59',
     '2024-10-25 23:59:59','2024-11-05 09:00:00',
     'CLOSED', (SELECT id FROM users WHERE username='truongkhoa')),

    ('Đợt TLCN học kỳ 2025-1', 'TLCN',
     '2025-01-15 00:00:00','2025-02-15 23:59:59',
     '2025-02-16 00:00:00','2025-03-20 23:59:59',
     '2025-04-30 23:59:59', NULL,
     'CLOSED', (SELECT id FROM users WHERE username='truongkhoa')),

    ('Đợt KLTN năm 2025', 'KLTN',
     '2025-09-01 00:00:00','2025-09-20 23:59:59',
     '2025-09-21 00:00:00','2025-10-15 23:59:59',
     '2025-10-25 23:59:59','2025-11-05 09:00:00',
     'CLOSED', (SELECT id FROM users WHERE username='truongkhoa')),

    -- Hiện tại (2026) - PUBLISHED
    ('Đợt TLCN học kỳ 2026-1', 'TLCN',
     '2026-01-15 00:00:00','2026-02-15 23:59:59',
     '2026-02-16 00:00:00','2026-03-20 23:59:59',
     '2026-04-30 23:59:59', NULL,
     'PUBLISHED', (SELECT id FROM users WHERE username='truongkhoa')),

    ('Đợt KLTN năm 2026', 'KLTN',
     '2026-09-01 00:00:00','2026-09-20 23:59:59',
     '2026-09-21 00:00:00','2026-10-15 23:59:59',
     '2026-10-25 23:59:59','2026-11-05 09:00:00',
     'PUBLISHED', (SELECT id FROM users WHERE username='truongkhoa')),

    -- Sắp tới - DRAFT
    ('Đợt TLCN học kỳ 2026-2', 'TLCN',
     '2026-08-15 00:00:00','2026-09-15 23:59:59',
     '2026-09-16 00:00:00','2026-10-20 23:59:59',
     '2026-11-30 23:59:59', NULL,
     'DRAFT', (SELECT id FROM users WHERE username='truongkhoa')),

    ('Đợt NCKH năm 2026', 'NCKH',
     '2026-03-01 00:00:00','2026-04-30 23:59:59',
     '2026-05-01 00:00:00','2026-06-30 23:59:59',
     NULL, NULL,
     'PUBLISHED', (SELECT id FROM users WHERE username='truongkhoa')),

    ('Đợt môn học 2026-1', 'MON_HOC',
     '2026-02-01 00:00:00','2026-02-28 23:59:59',
     '2026-03-01 00:00:00','2026-03-31 23:59:59',
     NULL, NULL,
     'CLOSED', (SELECT id FROM users WHERE username='truongkhoa')),

    ('Đợt môn học 2026-2', 'MON_HOC',
     '2026-08-01 00:00:00','2026-08-31 23:59:59',
     '2026-09-01 00:00:00','2026-09-30 23:59:59',
     NULL, NULL,
     'PUBLISHED', (SELECT id FROM users WHERE username='truongkhoa'));

-- ============================================================================
--  6.  Đề tài (~80)
--      Phân bổ:
--        - 30 đề tài đợt 2026-1 (TLCN) - 20 APPROVED published, 5 PENDING, 5 REJECTED
--        - 20 đề tài đợt 2026 KLTN - 15 APPROVED, 5 PENDING
--        - 10 đề tài đợt NCKH 2026 - 8 APPROVED, 2 PENDING
--        - 10 đề tài đợt môn học 2026-2 - 7 APPROVED, 3 PENDING
--        - 10 đề tài cũ (đã CLOSED) - tất cả APPROVED
--      CNPM ~ 30, KHMT ~ 30, MMT ~ 20
-- ============================================================================

-- Tạo đề tài bằng cách lặp: dùng bảng số 1..N
DROP TEMPORARY TABLE IF EXISTS tmp_n;
CREATE TEMPORARY TABLE tmp_n (n INT PRIMARY KEY);
INSERT INTO tmp_n (n)
SELECT a.N + b.N*10 + c.N*100 + 1
FROM (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
      UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a,
     (SELECT 0 AS N UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
      UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b,
     (SELECT 0 AS N UNION SELECT 1) c
WHERE a.N + b.N*10 + c.N*100 < 80;

-- Helper: sinh đề tài
DROP PROCEDURE IF EXISTS sp_seed_topics;
DELIMITER $$
CREATE PROCEDURE sp_seed_topics()
BEGIN
    DECLARE i INT DEFAULT 0;
    DECLARE v_period_id BIGINT UNSIGNED;
    DECLARE v_dept_code VARCHAR(20);
    DECLARE v_topic_code VARCHAR(40);
    DECLARE v_topic_name VARCHAR(255);
    DECLARE v_desc TEXT;
    DECLARE v_status VARCHAR(20);
    DECLARE v_lecturer_id BIGINT UNSIGNED;
    DECLARE v_approved_by BIGINT UNSIGNED;
    DECLARE v_approved_at DATETIME;
    DECLARE v_published TINYINT(1);
    DECLARE v_topic_count INT;

    DECLARE cur CURSOR FOR
        SELECT n FROM tmp_n ORDER BY n;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET i = 999;

    SET v_approved_by = (SELECT id FROM users WHERE username='truongkhoa' ORDER BY id ASC LIMIT 1);

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO v_topic_count;
        IF i = 999 THEN LEAVE read_loop; END IF;
        SET i = i + 1;

        -- Phân bổ đợt theo nhóm
        IF v_topic_count <= 30 THEN
            -- 30 đề tài đợt TLCN 2026-1
            SET v_period_id = (SELECT id FROM registration_periods WHERE name='Đợt TLCN học kỳ 2026-1' ORDER BY id ASC LIMIT 1);
        ELSEIF v_topic_count <= 50 THEN
            -- 20 đề tài đợt KLTN 2026
            SET v_period_id = (SELECT id FROM registration_periods WHERE name='Đợt KLTN năm 2026' ORDER BY id ASC LIMIT 1);
        ELSEIF v_topic_count <= 60 THEN
            -- 10 đề tài NCKH 2026
            SET v_period_id = (SELECT id FROM registration_periods WHERE name='Đợt NCKH năm 2026' ORDER BY id ASC LIMIT 1);
        ELSEIF v_topic_count <= 70 THEN
            -- 10 đề tài môn học 2026-2
            SET v_period_id = (SELECT id FROM registration_periods WHERE name='Đợt môn học 2026-2' ORDER BY id ASC LIMIT 1);
        ELSE
            -- 10 đề tài cũ
            SET v_period_id = (SELECT id FROM registration_periods WHERE name='Đợt KLTN năm 2025' ORDER BY id ASC LIMIT 1);
        END IF;

        -- Phân bổ bộ môn: 0-29 CNPM, 30-49 KHMT, 50-59 MMT, 60-69 CNPM, 70-79 KHMT
        IF v_topic_count <= 30 OR (v_topic_count >= 61 AND v_topic_count <= 70) THEN
            SET v_dept_code = 'CNPM';
        ELSEIF v_topic_count <= 50 OR v_topic_count >= 71 THEN
            SET v_dept_code = 'KHMT';
        ELSE
            SET v_dept_code = 'MMT';
        END IF;

        -- Mã đề tài
        SET v_topic_code = CONCAT('DT-', LPAD(v_topic_count, 4, '0'));

        -- Tên đề tài
        SET v_topic_name = CONCAT(
            CASE v_dept_code WHEN 'CNPM' THEN 'Xây dựng hệ thống ' WHEN 'KHMT' THEN 'Nghiên cứu thuật toán ' ELSE 'Triển khai hệ thống ' END,
            'chuyên đề ', v_topic_count
        );

        -- Mô tả ≥ 30 ký tự
        SET v_desc = CONCAT(
            'Đề tài số ', v_topic_count, ' thuộc đợt đăng ký, do giảng viên bộ môn ', v_dept_code, ' hướng dẫn. ',
            'Sinh viên thực hiện trong học kỳ hiện tại, đảm bảo các yêu cầu kỹ thuật và quy trình đã công bố.'
        );

        -- Trạng thái
        IF v_topic_count <= 30 THEN
            -- TLCN 2026-1: 20 APPROVED, 5 PENDING, 5 REJECTED
            IF v_topic_count <= 20 THEN
                SET v_status = 'APPROVED'; SET v_approved_at = '2026-02-10 10:00:00'; SET v_published = 1;
            ELSEIF v_topic_count <= 25 THEN
                SET v_status = 'PENDING'; SET v_approved_at = NULL; SET v_published = 0;
            ELSE
                SET v_status = 'REJECTED'; SET v_approved_at = NULL; SET v_published = 0;
            END IF;
        ELSEIF v_topic_count <= 50 THEN
            -- KLTN 2026: 15 APPROVED, 5 PENDING
            IF v_topic_count <= 45 THEN
                SET v_status = 'APPROVED'; SET v_approved_at = '2026-09-15 11:00:00'; SET v_published = 1;
            ELSE
                SET v_status = 'PENDING'; SET v_approved_at = NULL; SET v_published = 0;
            END IF;
        ELSEIF v_topic_count <= 60 THEN
            -- NCKH 2026: 8 APPROVED, 2 PENDING
            IF v_topic_count <= 58 THEN
                SET v_status = 'APPROVED'; SET v_approved_at = '2026-05-10 11:00:00'; SET v_published = 1;
            ELSE
                SET v_status = 'PENDING'; SET v_approved_at = NULL; SET v_published = 0;
            END IF;
        ELSEIF v_topic_count <= 70 THEN
            -- MON_HOC 2026-2: 7 APPROVED, 3 PENDING
            IF v_topic_count <= 67 THEN
                SET v_status = 'APPROVED'; SET v_approved_at = '2026-08-20 10:00:00'; SET v_published = 1;
            ELSE
                SET v_status = 'PENDING'; SET v_approved_at = NULL; SET v_published = 0;
            END IF;
        ELSE
            -- Cũ: APPROVED
            SET v_status = 'APPROVED'; SET v_approved_at = '2025-09-15 11:00:00'; SET v_published = 1;
        END IF;

        -- Chọn GV theo bộ môn (v_topic_count mod 10 + 1)
        SET v_topic_count = v_topic_count; -- giữ nguyên

        SET v_lecturer_id = (
            SELECT l.id FROM lecturers l
            JOIN users u ON u.id = l.user_id
            WHERE u.username COLLATE utf8mb4_unicode_ci = CONCAT('gv_', LOWER(v_dept_code), '_', LPAD(((v_topic_count - 1) MOD 10) + 1, 3, '0'))
            ORDER BY l.id ASC
            LIMIT 1
        );

        -- Insert topic
        INSERT INTO topics
            (period_id, department_id, created_by_lecturer_id, topic_code, topic_name, description,
             status, reject_reason, approved_by, approved_at, published, created_at, updated_at)
        VALUES
            (v_period_id,
             (SELECT id FROM departments WHERE code=v_dept_code),
             v_lecturer_id,
             v_topic_code, v_topic_name, v_desc,
             v_status,
             CASE WHEN v_status='REJECTED' THEN 'Đề tài chưa đáp ứng tiêu chí khoa học và thực tiễn' ELSE NULL END,
             CASE WHEN v_status='APPROVED' THEN v_approved_by ELSE NULL END,
             v_approved_at,
             v_published,
             NOW(), NOW())
        ON DUPLICATE KEY UPDATE topic_name = VALUES(topic_name);

        -- GVHD: GV chính (supervisor_order=1) + 1 số có GV phụ
        SET @v_topic_id := LAST_INSERT_ID();
        IF @v_topic_id > 0 THEN
            INSERT IGNORE INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
            VALUES (@v_topic_id, v_lecturer_id, 1);

            -- Một số đề tài có GVHD thứ hai
            IF v_topic_count MOD 3 = 0 THEN
                INSERT IGNORE INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
                SELECT @v_topic_id, l2.id, 2
                FROM lecturers l2
                JOIN users u2 ON u2.id = l2.user_id
                WHERE u2.username COLLATE utf8mb4_unicode_ci = CONCAT('gv_', LOWER(v_dept_code), '_', LPAD(((v_topic_count) MOD 10) + 1, 3, '0'))
                  AND l2.id != v_lecturer_id
                LIMIT 1;
            END IF;
        END IF;

    END LOOP;
    CLOSE cur;
END$$
DELIMITER ;

CALL sp_seed_topics();
DROP PROCEDURE sp_seed_topics;
DROP TEMPORARY TABLE tmp_n;

-- ============================================================================
--  7.  Nhóm SV (~25 nhóm) + topic_registrations
--      Mỗi nhóm 2-3 SV từ cùng lớp
--      Trạng thái: 10 APPROVED (gán đề tài), 10 PENDING, 5 DRAFT
-- ============================================================================

DROP TEMPORARY TABLE IF EXISTS tmp_groups;
CREATE TEMPORARY TABLE tmp_groups (
    n INT AUTO_INCREMENT PRIMARY KEY,
    period_name VARCHAR(180) NOT NULL,
    group_code VARCHAR(40) NOT NULL,
    group_name VARCHAR(180) NOT NULL,
    member1_username VARCHAR(60) NOT NULL,
    member2_username VARCHAR(60) NOT NULL,
    member3_username VARCHAR(60) NULL,
    status VARCHAR(20) NOT NULL,
    topic_code VARCHAR(40) NULL
);

INSERT INTO tmp_groups (period_name, group_code, group_name, member1_username, member2_username, member3_username, status, topic_code) VALUES
    -- 5 nhóm CNTT-K15
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT15-001', 'Nhóm CNTT15 - 1', 'sv_cntt15_001', 'sv_cntt15_002', 'sv_cntt15_003', 'APPROVED', 'DT-0001'),
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT15-002', 'Nhóm CNTT15 - 2', 'sv_cntt15_004', 'sv_cntt15_005', 'sv_cntt15_006', 'APPROVED', 'DT-0002'),
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT15-003', 'Nhóm CNTT15 - 3', 'sv_cntt15_007', 'sv_cntt15_008', NULL,             'PENDING',  'DT-0021'),
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT15-004', 'Nhóm CNTT15 - 4', 'sv_cntt15_009', 'sv_cntt15_010', 'sv_cntt15_011', 'DRAFT',    NULL),
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT15-005', 'Nhóm CNTT15 - 5', 'sv_cntt15_012', 'sv_cntt15_013', 'sv_cntt15_014', 'APPROVED', 'DT-0003'),

    -- 4 nhóm CNTT-K16
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT16-001', 'Nhóm CNTT16 - 1', 'sv_cntt16_001', 'sv_cntt16_002', 'sv_cntt16_003', 'APPROVED', 'DT-0004'),
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT16-002', 'Nhóm CNTT16 - 2', 'sv_cntt16_004', 'sv_cntt16_005', NULL,             'PENDING',  'DT-0022'),
    ('Đợt KLTN năm 2026',       'N-CNTT16-003', 'Nhóm CNTT16 - 3', 'sv_cntt16_006', 'sv_cntt16_007', 'sv_cntt16_008', 'APPROVED', 'DT-0031'),
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT16-004', 'Nhóm CNTT16 - 4', 'sv_cntt16_009', 'sv_cntt16_010', 'sv_cntt16_011', 'APPROVED', 'DT-0005'),

    -- 3 nhóm CNTT-K17
    ('Đợt KLTN năm 2026',       'N-CNTT17-001', 'Nhóm CNTT17 - 1', 'sv_cntt17_001', 'sv_cntt17_002', 'sv_cntt17_003', 'APPROVED', 'DT-0032'),
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT17-002', 'Nhóm CNTT17 - 2', 'sv_cntt17_004', 'sv_cntt17_005', NULL,             'PENDING',  'DT-0023'),
    ('Đợt KLTN năm 2026',       'N-CNTT17-003', 'Nhóm CNTT17 - 3', 'sv_cntt17_006', 'sv_cntt17_007', 'sv_cntt17_008', 'APPROVED', 'DT-0033'),

    -- 2 nhóm CNTT-K18
    ('Đợt TLCN học kỳ 2026-1', 'N-CNTT18-001', 'Nhóm CNTT18 - 1', 'sv_cntt18_001', 'sv_cntt18_002', 'sv_cntt18_003', 'APPROVED', 'DT-0006'),
    ('Đợt KLTN năm 2026',       'N-CNTT18-002', 'Nhóm CNTT18 - 2', 'sv_cntt18_004', 'sv_cntt18_005', 'sv_cntt18_006', 'DRAFT',    NULL),

    -- 4 nhóm KHMT
    ('Đợt TLCN học kỳ 2026-1', 'N-KHMT15-001', 'Nhóm KHMT15 - 1', 'sv_khmt15_001', 'sv_khmt15_002', 'sv_khmt15_003', 'APPROVED', 'DT-0034'),
    ('Đợt KLTN năm 2026',       'N-KHMT15-002', 'Nhóm KHMT15 - 2', 'sv_khmt15_004', 'sv_khmt15_005', NULL,             'PENDING',  'DT-0041'),
    ('Đợt TLCN học kỳ 2026-1', 'N-KHMT16-001', 'Nhóm KHMT16 - 1', 'sv_khmt16_001', 'sv_khmt16_002', 'sv_khmt16_003', 'APPROVED', 'DT-0035'),
    ('Đợt KLTN năm 2026',       'N-KHMT17-001', 'Nhóm KHMT17 - 1', 'sv_khmt17_001', 'sv_khmt17_002', 'sv_khmt17_003', 'APPROVED', 'DT-0036'),

    -- 3 nhóm MMT
    ('Đợt TLCN học kỳ 2026-1', 'N-MMT15-001',  'Nhóm MMT15 - 1',  'sv_mmt15_001',  'sv_mmt15_002',  'sv_mmt15_003',  'APPROVED', 'DT-0050'),
    ('Đợt KLTN năm 2026',       'N-MMT15-002',  'Nhóm MMT15 - 2',  'sv_mmt15_004',  'sv_mmt15_005',  NULL,              'PENDING',  'DT-0045'),
    ('Đợt TLCN học kỳ 2026-1', 'N-MMT15-003',  'Nhóm MMT15 - 3',  'sv_mmt15_006',  'sv_mmt15_007',  'sv_mmt15_008',  'DRAFT',    NULL),

    -- 4 nhóm cho các đợt khác (NCKH, MON_HOC)
    ('Đợt NCKH năm 2026',       'N-NCKH-001',   'Nhóm NCKH - 1',   'sv_cntt15_015', 'sv_cntt15_016', 'sv_cntt15_017', 'APPROVED', 'DT-0051'),
    ('Đợt NCKH năm 2026',       'N-NCKH-002',   'Nhóm NCKH - 2',   'sv_khmt15_006', 'sv_khmt15_007', NULL,             'PENDING',  'DT-0058'),
    ('Đợt môn học 2026-2',     'N-MH-001',     'Nhóm MH - 1',     'sv_cntt16_012', 'sv_cntt16_013', 'sv_cntt16_014', 'APPROVED', 'DT-0061'),
    ('Đợt môn học 2026-2',     'N-MH-002',     'Nhóm MH - 2',     'sv_khmt16_004', 'sv_khmt16_005', 'sv_khmt16_006', 'APPROVED', 'DT-0062');

-- Insert student_groups
INSERT IGNORE INTO student_groups (period_id, group_code, group_name, status)
SELECT p.id, g.group_code, g.group_name, g.status
FROM tmp_groups g JOIN registration_periods p ON p.name = g.period_name;

-- Gán topic_id cho các nhóm APPROVED
UPDATE student_groups sg
JOIN tmp_groups g ON sg.group_code = g.group_code
SET sg.topic_id = (SELECT t.id FROM topics t WHERE t.topic_code = g.topic_code)
WHERE g.status = 'APPROVED' AND g.topic_code IS NOT NULL;

-- Thêm group_members (bỏ qua nếu SV đã ở nhóm khác)
INSERT IGNORE INTO group_members (group_id, student_id, is_leader)
SELECT sg.id, s.id, 1
FROM tmp_groups g
JOIN student_groups sg ON sg.group_code = g.group_code
JOIN students s ON s.user_id = (SELECT id FROM users WHERE username = g.member1_username)
WHERE NOT EXISTS (SELECT 1 FROM group_members gm WHERE gm.student_id = s.id);

INSERT IGNORE INTO group_members (group_id, student_id, is_leader)
SELECT sg.id, s.id, 0
FROM tmp_groups g
JOIN student_groups sg ON sg.group_code = g.group_code
JOIN students s ON s.user_id = (SELECT id FROM users WHERE username = g.member2_username)
WHERE g.member2_username IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM group_members gm WHERE gm.student_id = s.id);

INSERT IGNORE INTO group_members (group_id, student_id, is_leader)
SELECT sg.id, s.id, 0
FROM tmp_groups g
JOIN student_groups sg ON sg.group_code = g.group_code
JOIN students s ON s.user_id = (SELECT id FROM users WHERE username = g.member3_username)
WHERE g.member3_username IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM group_members gm WHERE gm.student_id = s.id);

-- topic_registrations: cho các nhóm REGISTERED/APPROVED
INSERT IGNORE INTO topic_registrations (group_id, topic_id, status, approved_by, approved_at)
SELECT sg.id, t.id,
       CASE WHEN sg.status='APPROVED' THEN 'APPROVED' ELSE 'PENDING' END,
       CASE WHEN sg.status='APPROVED' THEN (SELECT id FROM users WHERE username='truongkhoa') ELSE NULL END,
       CASE WHEN sg.status='APPROVED' THEN NOW() ELSE NULL END
FROM tmp_groups g
JOIN student_groups sg ON sg.group_code = g.group_code
JOIN topics t ON t.topic_code = g.topic_code
WHERE g.topic_code IS NOT NULL;

DROP TEMPORARY TABLE tmp_groups;

-- ============================================================================
--  8.  5 Hội đồng + 15 council_members + 5 council_topics
--      Mỗi HĐ: 1 CHAIRMAN (cthd1/cthd2), 1 SECRETARY, 1-2 MEMBER
-- ============================================================================

INSERT IGNORE INTO councils (period_id, council_code, council_name, report_date, created_by, status)
SELECT
    (SELECT id FROM registration_periods WHERE name='Đợt KLTN năm 2026'),
    CONCAT('HD-2026-', LPAD(seq, 2, '0')),
    CONCAT('Hội đồng phản biện 2026 - ', seq),
    '2026-11-10 09:00:00' + INTERVAL seq HOUR,
    (SELECT id FROM users WHERE username='truongkhoa'),
    'READY'
FROM (SELECT 1 AS seq UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) s;

-- Council members: 1 CHAIRMAN, 1 SECRETARY, 1 MEMBER cho mỗi HĐ
-- Xoay vòng GV giữa các HĐ
INSERT IGNORE INTO council_members (council_id, lecturer_id, role)
SELECT c.id,
       (SELECT l.id FROM lecturers l JOIN users u ON u.id=l.user_id
        WHERE u.username COLLATE utf8mb4_unicode_ci = CASE (c.id MOD 5)
            WHEN 0 THEN 'gv_cnpm_001'
            WHEN 1 THEN 'gv_khmt_001'
            WHEN 2 THEN 'gv_mmt_001'
            WHEN 3 THEN 'gv_cnpm_005'
            ELSE 'gv_khmt_005'
        END),
       'CHAIRMAN'
FROM councils c
WHERE c.council_code LIKE 'HD-2026-%';

INSERT IGNORE INTO council_members (council_id, lecturer_id, role)
SELECT c.id,
       (SELECT l.id FROM lecturers l JOIN users u ON u.id=l.user_id
        WHERE u.username COLLATE utf8mb4_unicode_ci = CASE (c.id MOD 5)
            WHEN 0 THEN 'gv_cnpm_003'
            WHEN 1 THEN 'gv_khmt_003'
            WHEN 2 THEN 'gv_mmt_003'
            WHEN 3 THEN 'gv_cnpm_007'
            ELSE 'gv_khmt_007'
        END),
       'SECRETARY'
FROM councils c
WHERE c.council_code LIKE 'HD-2026-%';

INSERT IGNORE INTO council_members (council_id, lecturer_id, role)
SELECT c.id,
       (SELECT l.id FROM lecturers l JOIN users u ON u.id=l.user_id
        WHERE u.username COLLATE utf8mb4_unicode_ci = CASE (c.id MOD 5)
            WHEN 0 THEN 'gv_khmt_002'
            WHEN 1 THEN 'gv_mmt_002'
            WHEN 2 THEN 'gv_cnpm_002'
            WHEN 3 THEN 'gv_khmt_006'
            ELSE 'gv_mmt_006'
        END),
       'MEMBER'
FROM councils c
WHERE c.council_code LIKE 'HD-2026-%';

-- Gán 5 đề tài KLTN đã duyệt vào 5 HĐ
INSERT IGNORE INTO council_topics (council_id, topic_id)
SELECT c.id, t.id
FROM councils c
JOIN topics t ON t.topic_code = CONCAT('DT-', LPAD(30 + c.id - (SELECT MIN(id) FROM councils WHERE council_code LIKE 'HD-2026-%') + 1, 4, '0'))
WHERE c.council_code LIKE 'HD-2026-%'
  AND t.period_id = (SELECT id FROM registration_periods WHERE name='Đợt KLTN năm 2026')
  AND t.status = 'APPROVED'
ORDER BY c.id
LIMIT 5;

-- ============================================================================
--  9.  Review assignments + Evaluations + Results
--      Mỗi đề tài trong HĐ: 3 GV chấm → 3 evaluations → 1 review_result
-- ============================================================================

-- Phân công 3 GV chấm cho mỗi đề tài (lấy 3 GV từ council_members)
INSERT IGNORE INTO review_assignments (council_id, topic_id, lecturer_id, assigned_by, status)
SELECT ct.council_id, ct.topic_id, cm.lecturer_id,
       (SELECT id FROM users WHERE username='truongkhoa'),
       'ASSIGNED'
FROM council_topics ct
JOIN council_members cm ON cm.council_id = ct.council_id;

-- Nhập điểm: 5 đề tài × 3 GV = 15 evaluations
-- Điểm random hợp lệ 6.0 - 9.5
INSERT IGNORE INTO evaluations (assignment_id, score, comment, status, evaluated_at)
SELECT ra.id,
       ROUND(6.0 + RAND() * 3.5, 2),
       CONCAT('Đề tài có nội dung tốt, đáp ứng yêu cầu đề ra. Sinh viên trình bày rõ ràng, có sáng tạo. Điểm: ', ROUND(6.0 + RAND() * 3.5, 2)),
       'SUBMITTED',
       NOW()
FROM review_assignments ra;

-- Cập nhật assignment status
UPDATE review_assignments ra
JOIN evaluations e ON e.assignment_id = ra.id
SET ra.status = 'COMPLETED';

-- Tính review_results cho 5 nhóm KLTN đã có
INSERT IGNORE INTO review_results (group_id, topic_id, council_id, final_score, result, summary_comment, calculated_by, calculated_at, published, published_by, published_at)
SELECT sg.id, ct.topic_id, ct.council_id,
       ROUND(AVG(e.score), 2),
       CASE WHEN ROUND(AVG(e.score), 2) >= 5.00 THEN 'PASSED' ELSE 'FAILED' END,
       CONCAT('Tổng hợp điểm từ ', COUNT(*), ' giảng viên phản biện. Kết quả: ',
              CASE WHEN ROUND(AVG(e.score), 2) >= 5.00 THEN 'Đạt yêu cầu' ELSE 'Chưa đạt yêu cầu' END),
       (SELECT id FROM users WHERE username='truongkhoa'),
       NOW(),
       1,
       (SELECT id FROM users WHERE username='truongkhoa'),
       NOW()
FROM council_topics ct
JOIN student_groups sg ON sg.topic_id = ct.topic_id
JOIN review_assignments ra ON ra.council_id = ct.council_id AND ra.topic_id = ct.topic_id
JOIN evaluations e ON e.assignment_id = ra.id
GROUP BY sg.id, ct.topic_id, ct.council_id;

-- Cập nhật group status thành COMPLETED
UPDATE student_groups sg
JOIN review_results r ON r.group_id = sg.id
SET sg.status = 'COMPLETED';

-- ============================================================================
--  10a. Tài khoản demo tiện dụng (gv_demo, sv_demo) - dễ nhớ
-- ============================================================================

INSERT IGNORE INTO users (username, password, full_name, email, status, enabled) VALUES
    ('gv_demo', @pw, 'GV Demo (An)',  'gv.demo@cntt.edu.vn', 'ACTIVE', 1),
    ('sv_demo', @pw, 'SV Demo',        'sv.demo@cntt.edu.vn', 'ACTIVE', 1);

INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE (u.username='gv_demo' AND r.code='GIANG_VIEN')
   OR (u.username='sv_demo' AND r.code='SINH_VIEN');

-- Hồ sơ GV demo (gán về bộ môn CNPM)
INSERT IGNORE INTO lecturers (user_id, lecturer_code, academic_degree, department_id)
SELECT u.id, 'GV_DEMO', 'ThS',
       (SELECT id FROM departments WHERE code='CNPM')
FROM users u WHERE u.username='gv_demo';

-- Hồ sơ SV demo (lớp CNTT-K15)
INSERT IGNORE INTO students (user_id, student_code, class_name, major)
SELECT u.id, 'SVDEMO',
       'CNTT-K15', 'Công nghệ thông tin'
FROM users u WHERE u.username='sv_demo';

-- Gán gv_demo làm GVHD thứ 2 cho 1 đề tài APPROVED (để khi gv_demo login,
-- /registrations có dữ liệu để duyệt thử)
INSERT IGNORE INTO topic_supervisors (topic_id, lecturer_id, supervisor_order)
SELECT t.id, l.id, 2
FROM topics t
JOIN lecturers l
WHERE t.topic_code = 'DT-0001' AND l.lecturer_code = 'GV_DEMO'
  AND NOT EXISTS (
    SELECT 1 FROM topic_supervisors ts
    WHERE ts.topic_id = t.id AND ts.lecturer_id = l.id
  );

-- Tạo 1 nhóm nhỏ do sv_demo làm leader, đăng ký đề tài do gv_demo hướng dẫn
INSERT IGNORE INTO student_groups (period_id, group_code, group_name, status)
SELECT (SELECT id FROM registration_periods WHERE name='Đợt TLCN học kỳ 2026-1' ORDER BY id ASC LIMIT 1),
       'N-DEMO-01', 'Nhóm Demo', 'REGISTERED';

-- Xóa leader cũ nếu có (do seed chạy lại nhiều lần)
DELETE FROM group_members
WHERE student_id = (SELECT id FROM students WHERE student_code='SVDEMO')
  AND group_id = (SELECT id FROM student_groups WHERE group_code='N-DEMO-01');

INSERT IGNORE INTO group_members (group_id, student_id, is_leader)
SELECT sg.id, s.id, 1
FROM student_groups sg, students s
JOIN users u ON u.id = s.user_id
WHERE sg.group_code = 'N-DEMO-01' AND u.username = 'sv_demo';

INSERT IGNORE INTO topic_registrations (group_id, topic_id, status)
SELECT sg.id, t.id, 'PENDING'
FROM student_groups sg, topics t
WHERE sg.group_code = 'N-DEMO-01' AND t.topic_code = 'DT-0001';

-- ============================================================================
--  10. Thông báo (10)
-- ============================================================================

INSERT IGNORE INTO notifications (title, content, type, created_by, status, published_at)
VALUES
    ('Lịch bảo vệ KLTN 2026',
     'Lịch bảo vệ các đề tài KLTN năm 2026 sẽ diễn ra từ ngày 10/11/2026. Sinh viên chuẩn bị báo cáo và slide đầy đủ.',
     'FACULTY', (SELECT id FROM users WHERE username='truongkhoa'), 'PUBLISHED', '2026-10-01 09:00:00'),

    ('Nộp báo cáo giữa kỳ TLCN',
     'Sinh viên làm tiểu luận chuyên ngành nộp báo cáo giữa kỳ trước ngày 30/04/2026 qua hệ thống.',
     'FACULTY', (SELECT id FROM users WHERE username='truongkhoa'), 'PUBLISHED', '2026-03-01 09:00:00'),

    ('Hướng dẫn đăng ký đề tài',
     'Sinh viên lập nhóm 2-3 người cùng lớp, đăng ký đề tài đã được duyệt và công bố. Mỗi SV chỉ thuộc 1 nhóm.',
     'SCHOOL', (SELECT id FROM users WHERE username='admin'), 'PUBLISHED', '2026-02-15 10:00:00'),

    ('Thông báo lịch họp HĐ phản biện',
     'Các thành viên hội đồng phản biện họp lúc 14h ngày 08/11/2026 tại phòng A1-301 để phân công chấm.',
     'FACULTY', (SELECT id FROM users WHERE username='truongkhoa'), 'PUBLISHED', '2026-10-25 14:00:00'),

    ('Thay đổi thời hạn nộp báo cáo',
     'Thời hạn nộp báo cáo cuối kỳ được gia hạn đến 15/11/2026. Sinh viên lưu ý để hoàn thành đúng hạn.',
     'FACULTY', (SELECT id FROM users WHERE username='truongkhoa'), 'PUBLISHED', '2026-10-20 09:00:00'),

    ('Hội thảo NCKH sinh viên 2026',
     'Hội thảo khoa học sinh viên năm 2026 sẽ tổ chức ngày 20/12/2026. Khuyến khích SV đăng ký báo cáo.',
     'SCHOOL', (SELECT id FROM users WHERE username='admin'), 'PUBLISHED', '2026-10-10 09:00:00'),

    ('Kết quả KLTN 2024 đã công bố',
     'Kết quả KLTN năm 2024 đã được công bố. Sinh viên truy cập mục Kết quả của tôi để xem chi tiết.',
     'FACULTY', (SELECT id FROM users WHERE username='truongkhoa'), 'PUBLISHED', '2024-11-15 10:00:00'),

    ('Hướng dẫn sử dụng hệ thống',
     'Tài liệu hướng dẫn sử dụng hệ thống quản lý đề tài đã được cập nhật. Xem tại menu Hỗ trợ.',
     'SCHOOL', (SELECT id FROM users WHERE username='admin'), 'PUBLISHED', '2026-01-15 10:00:00'),

    ('Thông báo bảo trì hệ thống',
     'Hệ thống sẽ bảo trì từ 00:00 - 02:00 ngày 25/10/2026. Vui lòng không thao tác trong thời gian này.',
     'SCHOOL', (SELECT id FROM users WHERE username='admin'), 'PUBLISHED', '2026-10-22 10:00:00'),

    ('Quy định về đạo văn trong đề tài',
     'Khoa CNTT nghiêm cấm mọi hành vi đạo văn. Đề tài vi phạm sẽ bị huỷ kết quả và xử lý theo quy chế.',
     'FACULTY', (SELECT id FROM users WHERE username='truongkhoa'), 'PUBLISHED', '2026-02-01 09:00:00');

-- Gửi thông báo cho tất cả role (rỗng = tất cả)
INSERT IGNORE INTO notification_target_roles (notification_id, role_id)
SELECT n.id, r.id FROM notifications n CROSS JOIN roles r;

-- ============================================================================
--  Kết thúc
-- ============================================================================
SELECT 'Seed hoàn tất' AS status;
SELECT
    (SELECT COUNT(*) FROM users WHERE username LIKE 'sv\\_%' COLLATE utf8mb4_unicode_ci) AS so_sinh_vien,
    (SELECT COUNT(*) FROM users WHERE username LIKE 'gv\\_%' COLLATE utf8mb4_unicode_ci) AS so_giang_vien,
    (SELECT COUNT(*) FROM topics) AS so_de_tai,
    (SELECT COUNT(*) FROM student_groups) AS so_nhom,
    (SELECT COUNT(*) FROM councils WHERE council_code LIKE 'HD-2026-%') AS so_hoi_dong,
    (SELECT COUNT(*) FROM review_results) AS so_ket_qua,
    (SELECT COUNT(*) FROM notifications) AS so_thong_bao;






