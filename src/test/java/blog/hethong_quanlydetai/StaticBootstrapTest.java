package blog.hethong_quanlydetai;

import org.junit.jupiter.api.Test;

/**
 * Giai đoạn 1: kiểm tra tối thiểu rằng context-load và các hằng số quan trọng
 * còn dùng được (không cần database vì chỉ test các lớp không phụ thuộc Spring).
 *
 * Lưu ý: chưa load full Spring context để tránh fail khi MySQL chưa có.
 * Các giai đoạn sau sẽ dùng @SpringBootTest với H2/MySQL test.
 */
class StaticBootstrapTest {

    @Test
    void packageConstantCompiles() {
        // Đảm bảo enum PermissionCodes có đủ các mã quyền theo đề bài mục 3.
        String[] expected = {
            "USER_MANAGE","ROLE_MANAGE","PERMISSION_MANAGE",
            "PERIOD_MANAGE","TOPIC_VIEW","TOPIC_CREATE","TOPIC_APPROVE",
            "REGISTRATION_CREATE","REGISTRATION_CONFIRM","SUBMISSION_CREATE",
            "REPORT_VIEW","COUNCIL_MANAGE","ASSIGNMENT_MANAGE",
            "EVALUATION_CREATE","RESULT_CALCULATE","RESULT_PUBLISH",
            "RESULT_VIEW","NOTICE_VIEW","NOTICE_MANAGE"
        };
        for (String code : expected) {
            try {
                Class<?> c = Class.forName("blog.hethong_quanlydetai.domain.PermissionCodes");
                c.getField(code);
            } catch (NoSuchFieldException e) {
                throw new AssertionError("Thiếu hằng số PermissionCodes." + code, e);
            } catch (ClassNotFoundException e) {
                throw new AssertionError(e);
            }
        }
    }

    @Test
    void periodTypeRequiresFieldsAreCorrect() {
        org.junit.jupiter.api.Assertions.assertTrue(
            blog.hethong_quanlydetai.domain.PeriodType.TLCN.requiresReviewDeadline());
        org.junit.jupiter.api.Assertions.assertTrue(
            blog.hethong_quanlydetai.domain.PeriodType.KLTN.requiresReviewDeadline());
        org.junit.jupiter.api.Assertions.assertFalse(
            blog.hethong_quanlydetai.domain.PeriodType.MON_HOC.requiresReviewDeadline());
        org.junit.jupiter.api.Assertions.assertTrue(
            blog.hethong_quanlydetai.domain.PeriodType.KLTN.requiresCouncilReportDate());
        org.junit.jupiter.api.Assertions.assertFalse(
            blog.hethong_quanlydetai.domain.PeriodType.TLCN.requiresCouncilReportDate());
    }
}