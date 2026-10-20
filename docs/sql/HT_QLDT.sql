-- ============================================================================
--  HT_QLDT.sql  -  Hệ thống quản lý đề tài sinh viên (khoa CNTT)
--  Schema nguồn chuẩn. Chạy sạch trên MySQL 8.0+ trống.
--  Có thể mở trong MySQL Workbench: File > Run SQL Script.
--  Tên ràng buộc đặt rõ để vẽ sơ đồ EER bằng Reverse Engineer.
-- ============================================================================

-- Nạp lại an toàn ------------------------------------------------------------
DROP DATABASE IF EXISTS hethong_quanlydetai;
CREATE DATABASE hethong_quanlydetai
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE hethong_quanlydetai;

-- Bật khóa ngoại & kiểm tra (mặc định nhưng ghi rõ cho Workbench)
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
--  1.  Phân quyền & người dùng
-- ============================================================================

CREATE TABLE roles (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(40)     NOT NULL,
    name            VARCHAR(120)    NOT NULL,
    description     VARCHAR(255)    NULL,
    system_flag     TINYINT(1)      NOT NULL DEFAULT 0,    -- vai trò hệ thống không xóa được
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_roles_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permissions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(60)     NOT NULL,                -- vd USER_MANAGE, TOPIC_APPROVE
    name            VARCHAR(160)    NOT NULL,
    description     VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_permissions_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username        VARCHAR(60)     NOT NULL,
    password        VARCHAR(255)    NOT NULL,                -- BCrypt hash
    full_name       VARCHAR(150)    NOT NULL,
    email           VARCHAR(160)    NOT NULL,
    phone           VARCHAR(20)     NULL,
    status          ENUM('ACTIVE','LOCKED') NOT NULL DEFAULT 'ACTIVE',
    enabled         TINYINT(1)      NOT NULL DEFAULT 1,
    failed_attempts INT             NOT NULL DEFAULT 0,
    locked_until    DATETIME        NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_roles (
    user_id         BIGINT UNSIGNED NOT NULL,
    role_id         BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permissions (
    role_id         BIGINT UNSIGNED NOT NULL,
    permission_id   BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_perm_role FOREIGN KEY (role_id) REFERENCES roles(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_role_perm_perm FOREIGN KEY (permission_id) REFERENCES permissions(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  2.  Bộ môn & hồ sơ sinh viên / giảng viên
-- ============================================================================

CREATE TABLE departments (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(20)     NOT NULL,
    name            VARCHAR(150)    NOT NULL,
    status          VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_departments_code UNIQUE (code),
    CONSTRAINT ck_departments_status CHECK (status IN ('ACTIVE','INACTIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE students (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    student_code    VARCHAR(20)     NOT NULL,
    class_name      VARCHAR(60)     NOT NULL,
    major           VARCHAR(120)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_students_user UNIQUE (user_id),
    CONSTRAINT uq_students_code UNIQUE (student_code),
    CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE lecturers (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    lecturer_code   VARCHAR(20)     NOT NULL,
    academic_degree VARCHAR(80)     NULL,                    -- ThS, TS, PGS, GS...
    department_id   BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_lecturers_user UNIQUE (user_id),
    CONSTRAINT uq_lecturers_code UNIQUE (lecturer_code),
    CONSTRAINT fk_lecturers_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_lecturers_dept FOREIGN KEY (department_id) REFERENCES departments(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  3.  Đợt đăng ký
-- ============================================================================

CREATE TABLE registration_periods (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name                VARCHAR(180)    NOT NULL,
    period_type         ENUM('MON_HOC','NCKH','TLCN','KLTN') NOT NULL,
    lecturer_start_at   DATETIME        NOT NULL,
    lecturer_end_at     DATETIME        NOT NULL,
    student_start_at    DATETIME        NOT NULL,
    student_end_at      DATETIME        NOT NULL,
    review_deadline     DATETIME        NULL,                -- chỉ TLCN/KLTN
    council_report_date DATETIME        NULL,                -- chỉ KLTN
    status              ENUM('DRAFT','PUBLISHED','CLOSED') NOT NULL DEFAULT 'DRAFT',
    created_by          BIGINT UNSIGNED NOT NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_periods_creator FOREIGN KEY (created_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_period_time_order        CHECK (lecturer_start_at < lecturer_end_at
                                                  AND student_start_at < student_end_at),
    CONSTRAINT ck_period_student_after_gv  CHECK (student_start_at >= lecturer_end_at),
    CONSTRAINT ck_period_review_only_tlcn  CHECK (
            (period_type IN ('TLCN','KLTN') AND review_deadline IS NOT NULL)
         OR (period_type NOT IN ('TLCN','KLTN') AND review_deadline IS NULL)
    ),
    CONSTRAINT ck_period_council_only_kltn CHECK (
            (period_type = 'KLTN' AND council_report_date IS NOT NULL)
         OR (period_type <> 'KLTN' AND council_report_date IS NULL)
    ),
    CONSTRAINT ck_period_review_after_student CHECK (review_deadline IS NULL OR review_deadline > student_end_at),
    CONSTRAINT ck_period_council_after_review CHECK (council_report_date IS NULL OR council_report_date > review_deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  4.  Đề tài & giảng viên hướng dẫn
-- ============================================================================

CREATE TABLE topics (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    period_id               BIGINT UNSIGNED NOT NULL,
    department_id           BIGINT UNSIGNED NOT NULL,
    created_by_lecturer_id  BIGINT UNSIGNED NOT NULL,
    topic_code              VARCHAR(40)     NOT NULL,
    topic_name              VARCHAR(255)    NOT NULL,
    description             TEXT            NOT NULL,        -- bắt buộc, độ dài tối thiểu (kiểm tra ở service)
    status                  ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
    reject_reason           VARCHAR(500)    NULL,
    approved_by             BIGINT UNSIGNED NULL,
    approved_at             DATETIME        NULL,
    published               TINYINT(1)      NOT NULL DEFAULT 0, -- công bố cho SV xem
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_topics_code UNIQUE (topic_code),
    CONSTRAINT fk_topics_period  FOREIGN KEY (period_id)              REFERENCES registration_periods(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_topics_dept    FOREIGN KEY (department_id)          REFERENCES departments(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_topics_creator FOREIGN KEY (created_by_lecturer_id) REFERENCES lecturers(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_topics_approver FOREIGN KEY (approved_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_topics_description_len CHECK (CHAR_LENGTH(description) >= 30)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE topic_supervisors (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    topic_id         BIGINT UNSIGNED NOT NULL,
    lecturer_id      BIGINT UNSIGNED NOT NULL,
    supervisor_order INT             NOT NULL,                -- 1 hoặc 2
    PRIMARY KEY (id),
    CONSTRAINT uq_topic_supervisor_topic_lect UNIQUE (topic_id, lecturer_id),
    CONSTRAINT uq_topic_supervisor_topic_ord  UNIQUE (topic_id, supervisor_order),
    CONSTRAINT fk_topic_sup_topic    FOREIGN KEY (topic_id)    REFERENCES topics(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_topic_sup_lecturer FOREIGN KEY (lecturer_id) REFERENCES lecturers(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_topic_sup_order CHECK (supervisor_order IN (1,2))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  5.  Nhóm sinh viên & đăng ký đề tài
-- ============================================================================

CREATE TABLE student_groups (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    period_id    BIGINT UNSIGNED NOT NULL,
    group_code   VARCHAR(40)     NOT NULL,
    group_name   VARCHAR(180)    NOT NULL,
    topic_id     BIGINT UNSIGNED NULL,                       -- gán khi được GVHD chấp thuận
    status       ENUM('DRAFT','REGISTERED','APPROVED','SUBMITTED','COMPLETED') NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_group_code UNIQUE (group_code),
    CONSTRAINT uq_group_topic UNIQUE (topic_id),             -- mỗi đề tài tối đa 1 nhóm được gán
    CONSTRAINT fk_group_period FOREIGN KEY (period_id) REFERENCES registration_periods(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_group_topic  FOREIGN KEY (topic_id)   REFERENCES topics(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE group_members (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    group_id    BIGINT UNSIGNED NOT NULL,
    student_id  BIGINT UNSIGNED NOT NULL,
    is_leader   TINYINT(1)      NOT NULL DEFAULT 0,
    joined_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_gm_student  UNIQUE (student_id),           -- mỗi SV chỉ một nhóm
    CONSTRAINT uq_gm_pair     UNIQUE (group_id, student_id),
    CONSTRAINT fk_gm_group   FOREIGN KEY (group_id)   REFERENCES student_groups(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_gm_student FOREIGN KEY (student_id) REFERENCES students(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Đảm bảo mỗi nhóm đúng 1 nhóm trưởng (trigger)
DROP TRIGGER IF EXISTS trg_group_members_leader;
DELIMITER $$
CREATE TRIGGER trg_group_members_leader
BEFORE INSERT ON group_members
FOR EACH ROW
BEGIN
    DECLARE v_count INT;
    IF NEW.is_leader = 1 THEN
        SELECT COUNT(*) INTO v_count
        FROM group_members
        WHERE group_id = NEW.group_id AND is_leader = 1;
        IF v_count >= 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Nhóm đã có nhóm trưởng';
        END IF;
    END IF;
END$$
DELIMITER ;

CREATE TABLE topic_registrations (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    group_id          BIGINT UNSIGNED NOT NULL,
    topic_id          BIGINT UNSIGNED NOT NULL,
    status            ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    registered_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by       BIGINT UNSIGNED NULL,
    approved_at       DATETIME        NULL,
    rejection_reason  VARCHAR(500)    NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reg_group FOREIGN KEY (group_id) REFERENCES student_groups(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_reg_topic FOREIGN KEY (topic_id) REFERENCES topics(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_reg_approver FOREIGN KEY (approved_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_reg_topic_status ON topic_registrations(topic_id, status);
CREATE INDEX idx_reg_group_status ON topic_registrations(group_id, status);

-- ============================================================================
--  6.  Báo cáo
-- ============================================================================

CREATE TABLE reports (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    group_id      BIGINT UNSIGNED NOT NULL,
    submitted_by  BIGINT UNSIGNED NOT NULL,                  -- users.id của nhóm trưởng
    report_title  VARCHAR(255)    NOT NULL,
    file_name     VARCHAR(255)    NOT NULL,                  -- tên gốc
    stored_path   VARCHAR(500)    NOT NULL,                  -- đường dẫn thực lưu
    file_size     BIGINT UNSIGNED NOT NULL,                  -- bytes
    version       INT UNSIGNED    NOT NULL,                  -- tăng dần trong cùng group
    description   TEXT            NULL,
    submitted_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        VARCHAR(12)     NOT NULL DEFAULT 'SUBMITTED',
    PRIMARY KEY (id),
    CONSTRAINT uq_report_group_version UNIQUE (group_id, version),
    CONSTRAINT fk_report_group FOREIGN KEY (group_id) REFERENCES student_groups(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_report_user  FOREIGN KEY (submitted_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_report_version_pos CHECK (version >= 1),
    CONSTRAINT ck_report_status CHECK (status IN ('SUBMITTED','SUPERSEDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  7.  Hội đồng phản biện, phân công chấm, nhập điểm
-- ============================================================================

CREATE TABLE councils (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    period_id     BIGINT UNSIGNED NOT NULL,
    council_code  VARCHAR(40)     NOT NULL,
    council_name  VARCHAR(180)    NOT NULL,
    report_date   DATETIME        NULL,                      -- ngày báo cáo (NULL nếu không KLTN)
    created_by    BIGINT UNSIGNED NOT NULL,
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status        ENUM('DRAFT','READY','FINISHED') NOT NULL DEFAULT 'DRAFT',
    PRIMARY KEY (id),
    CONSTRAINT uq_council_code UNIQUE (council_code),
    CONSTRAINT fk_council_period FOREIGN KEY (period_id) REFERENCES registration_periods(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_council_creator FOREIGN KEY (created_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE council_members (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    council_id   BIGINT UNSIGNED NOT NULL,
    lecturer_id  BIGINT UNSIGNED NOT NULL,
    role         ENUM('CHAIRMAN','SECRETARY','MEMBER') NOT NULL DEFAULT 'MEMBER',
    PRIMARY KEY (id),
    CONSTRAINT uq_cm_council_lecturer UNIQUE (council_id, lecturer_id),
    CONSTRAINT fk_cm_council  FOREIGN KEY (council_id)  REFERENCES councils(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_cm_lecturer FOREIGN KEY (lecturer_id) REFERENCES lecturers(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE council_topics (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    council_id   BIGINT UNSIGNED NOT NULL,
    topic_id     BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_council_topic UNIQUE (topic_id),           -- mỗi đề tài chỉ vào 1 hội đồng
    CONSTRAINT fk_ct_council FOREIGN KEY (council_id) REFERENCES councils(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ct_topic   FOREIGN KEY (topic_id)   REFERENCES topics(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE review_assignments (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    council_id    BIGINT UNSIGNED NOT NULL,
    topic_id      BIGINT UNSIGNED NOT NULL,
    lecturer_id   BIGINT UNSIGNED NOT NULL,
    assigned_by   BIGINT UNSIGNED NOT NULL,
    assigned_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        VARCHAR(12)     NOT NULL DEFAULT 'ASSIGNED',
    PRIMARY KEY (id),
    CONSTRAINT uq_review_assignment UNIQUE (council_id, topic_id, lecturer_id),
    CONSTRAINT fk_ra_council  FOREIGN KEY (council_id)  REFERENCES councils(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ra_topic    FOREIGN KEY (topic_id)    REFERENCES topics(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ra_lecturer FOREIGN KEY (lecturer_id) REFERENCES lecturers(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_ra_assigner FOREIGN KEY (assigned_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT ck_review_assignment_status CHECK (status IN ('ASSIGNED','COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE evaluations (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assignment_id  BIGINT UNSIGNED NOT NULL,
    score          DECIMAL(4,2)    NOT NULL,
    comment        TEXT            NOT NULL,
    evaluated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status         VARCHAR(12)     NOT NULL DEFAULT 'SUBMITTED',
    PRIMARY KEY (id),
    CONSTRAINT uq_eval_assignment UNIQUE (assignment_id),
    CONSTRAINT fk_eval_assignment FOREIGN KEY (assignment_id) REFERENCES review_assignments(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT ck_eval_score_range CHECK (score >= 0 AND score <= 10),
    CONSTRAINT ck_evaluation_status CHECK (status IN ('DRAFT','SUBMITTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  8.  Kết quả phản biện
-- ============================================================================

CREATE TABLE review_results (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    group_id        BIGINT UNSIGNED NOT NULL,
    topic_id        BIGINT UNSIGNED NOT NULL,
    council_id      BIGINT UNSIGNED NOT NULL,
    final_score     DECIMAL(4,2)    NULL,
    result          ENUM('PASSED','FAILED') NULL,
    summary_comment TEXT            NULL,
    calculated_by   BIGINT UNSIGNED NULL,
    calculated_at   DATETIME        NULL,
    published       TINYINT(1)      NOT NULL DEFAULT 0,
    published_by    BIGINT UNSIGNED NULL,
    published_at    DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_result_group UNIQUE (group_id),
    CONSTRAINT uq_result_topic UNIQUE (topic_id),
    CONSTRAINT fk_result_group   FOREIGN KEY (group_id)   REFERENCES student_groups(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_result_topic   FOREIGN KEY (topic_id)   REFERENCES topics(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_result_council FOREIGN KEY (council_id) REFERENCES councils(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_result_calc    FOREIGN KEY (calculated_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_result_pub     FOREIGN KEY (published_by)  REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT ck_result_score_range CHECK (final_score IS NULL OR (final_score >= 0 AND final_score <= 10))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  9.  Thông báo
-- ============================================================================

CREATE TABLE notifications (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    title        VARCHAR(255)    NOT NULL,
    content      TEXT            NOT NULL,
    type         ENUM('SCHOOL','FACULTY') NOT NULL DEFAULT 'FACULTY',
    created_by   BIGINT UNSIGNED NOT NULL,
    status       ENUM('DRAFT','PUBLISHED','HIDDEN') NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at DATETIME        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notif_creator FOREIGN KEY (created_by) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notification_target_roles (
    notification_id BIGINT UNSIGNED NOT NULL,
    role_id         BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (notification_id, role_id),
    CONSTRAINT fk_ntr_notif FOREIGN KEY (notification_id) REFERENCES notifications(id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ntr_role  FOREIGN KEY (role_id)         REFERENCES roles(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  10.  Nhật ký
-- ============================================================================

CREATE TABLE audit_logs (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NULL,                        -- NULL nếu hệ thống/ẩn danh
    action      VARCHAR(80)     NOT NULL,                     -- vd TOPIC_APPROVE
    entity      VARCHAR(60)     NOT NULL,                     -- vd Topic
    entity_id   BIGINT UNSIGNED NULL,
    detail      TEXT            NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_entity (entity, entity_id),
    KEY idx_audit_user (user_id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
--  11.  Dữ liệu nền tảng: vai trò & quyền (idempotent)
--       Các tập mã quyền này được code nạp lại khi khởi động (CommandLineRunner).
--       Ở đây chèn tối thiểu để script chạy sạch trên DB trống.
-- ============================================================================

INSERT INTO roles (code, name, description, system_flag) VALUES
    ('ADMIN',              'Quản trị hệ thống',     'Quản trị viên',            1),
    ('TRUONG_KHOA',        'Trưởng khoa',           'Trưởng khoa CNTT',        1),
    ('CHU_TICH_HOI_DONG',  'Chủ tịch hội đồng',     'Chủ tịch HĐ phản biện',  1),
    ('GIANG_VIEN',         'Giảng viên',            'Giảng viên khoa CNTT',    1),
    ('SINH_VIEN',          'Sinh viên',             'Sinh viên khoa CNTT',     1);

-- Quyền khởi tạo cho 5 vai trò
INSERT INTO permissions (code, name, description) VALUES
    ('USER_MANAGE',         'Quản lý người dùng',           'Tạo/sửa/khóa tài khoản'),
    ('ROLE_MANAGE',          'Quản lý vai trò',              'Tạo/sửa vai trò'),
    ('PERMISSION_MANAGE',   'Phân quyền',                   'Gán/bỏ quyền cho vai trò'),
    ('PERIOD_MANAGE',       'Quản lý đợt đăng ký',          'Tạo/sửa/công bố/đóng đợt'),
    ('TOPIC_VIEW',          'Xem đề tài',                   'Xem danh sách/chi tiết đề tài'),
    ('TOPIC_CREATE',        'Đề xuất đề tài',               'GV đề xuất và sửa đề tài của mình'),
    ('TOPIC_APPROVE',       'Duyệt đề tài',                 'Duyệt/từ chối đề tài'),
    ('REGISTRATION_CREATE', 'Lập nhóm và đăng ký đề tài',   'SV lập nhóm, đăng ký/huỷ đăng ký'),
    ('REGISTRATION_CONFIRM','Chấp thuận nhóm đăng ký',      'GVHD chấp thuận nhóm đăng ký đề tài'),
    ('SUBMISSION_CREATE',   'Nộp báo cáo',                  'Nhóm trưởng nộp báo cáo'),
    ('REPORT_VIEW',         'Xem báo cáo',                  'Xem/tải báo cáo theo quyền'),
    ('COUNCIL_MANAGE',      'Lập hội đồng phản biện',       'Tạo/sửa hội đồng, chọn thành viên'),
    ('ASSIGNMENT_MANAGE',    'Phân công chấm',               'Phân công người chấm theo đề tài'),
    ('EVALUATION_CREATE',   'Nhập điểm',                    'GV nhập điểm đề tài được phân công'),
    ('RESULT_CALCULATE',    'Tổng hợp điểm',                'Tổng hợp điểm & đưa ra Đạt/Không đạt'),
    ('RESULT_PUBLISH',      'Công bố kết quả',              'Công bố kết quả cho SV'),
    ('RESULT_VIEW',         'Xem kết quả của tôi',          'SV xem kết quả nhóm đã công bố'),
    ('NOTICE_VIEW',         'Xem thông báo',                'Xem thông báo dành cho vai trò'),
    ('NOTICE_MANAGE',       'Quản lý thông báo',            'Đăng/sửa/thu hồi thông báo');

-- Gán quyền mặc định cho 5 vai trò hệ thống (khớp bảng §3)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE 1=0;  -- placeholder, ánh xạ thực tế dưới đây

-- ADMIN: tất cả trừ EVALUATION_CREATE, REGISTRATION_CREATE, REGISTRATION_CONFIRM, SUBMISSION_CREATE (vẫn cho nhưng không dùng)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'ADMIN' AND p.code IN (
  'USER_MANAGE','ROLE_MANAGE','PERMISSION_MANAGE',
  'PERIOD_MANAGE','TOPIC_VIEW','TOPIC_APPROVE',
  'REGISTRATION_CONFIRM','SUBMISSION_CREATE','REPORT_VIEW',
  'COUNCIL_MANAGE','ASSIGNMENT_MANAGE','EVALUATION_CREATE',
  'RESULT_CALCULATE','RESULT_PUBLISH','RESULT_VIEW',
  'NOTICE_VIEW','NOTICE_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'TRUONG_KHOA' AND p.code IN (
  'PERIOD_MANAGE','TOPIC_VIEW','TOPIC_APPROVE',
  'REPORT_VIEW','COUNCIL_MANAGE','RESULT_PUBLISH',
  'NOTICE_VIEW','NOTICE_MANAGE'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'CHU_TICH_HOI_DONG' AND p.code IN (
  'TOPIC_VIEW','REPORT_VIEW','ASSIGNMENT_MANAGE',
  'EVALUATION_CREATE','RESULT_CALCULATE','NOTICE_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'GIANG_VIEN' AND p.code IN (
  'TOPIC_VIEW','TOPIC_CREATE','REGISTRATION_CONFIRM',
  'REPORT_VIEW','EVALUATION_CREATE','NOTICE_VIEW'
);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.code = 'SINH_VIEN' AND p.code IN (
  'TOPIC_VIEW','REGISTRATION_CREATE','SUBMISSION_CREATE',
  'REPORT_VIEW','RESULT_VIEW','NOTICE_VIEW'
);

-- ============================================================================
--  Kết thúc schema.
--  Tài khoản admin mặc định và dữ liệu demo được nạp bằng HT_QLDT_demo.sql
--  (chạy riêng khi profile=demo).
-- ============================================================================