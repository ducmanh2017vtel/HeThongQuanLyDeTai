package blog.hethong_quanlydetai.domain;

/** Vai trò hệ thống (khớp bảng roles). */
public enum RoleCode {
    ADMIN,
    TRUONG_KHOA,
    CHU_TICH_HOI_DONG,
    GIANG_VIEN,
    SINH_VIEN;

    public static RoleCode from(String code) {
        if (code == null) return null;
        return RoleCode.valueOf(code.trim().toUpperCase());
    }
}