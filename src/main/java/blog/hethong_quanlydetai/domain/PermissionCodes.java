package blog.hethong_quanlydetai.domain;

/** Mã quyền (khớp bảng permissions). */
public final class PermissionCodes {

    private PermissionCodes() {}

    public static final String USER_MANAGE          = "USER_MANAGE";
    public static final String ROLE_MANAGE          = "ROLE_MANAGE";
    public static final String PERMISSION_MANAGE    = "PERMISSION_MANAGE";
    public static final String PERIOD_MANAGE        = "PERIOD_MANAGE";
    public static final String TOPIC_VIEW           = "TOPIC_VIEW";
    public static final String TOPIC_CREATE         = "TOPIC_CREATE";
    public static final String TOPIC_APPROVE        = "TOPIC_APPROVE";
    public static final String REGISTRATION_CREATE  = "REGISTRATION_CREATE";
    public static final String REGISTRATION_CONFIRM = "REGISTRATION_CONFIRM";
    public static final String SUBMISSION_CREATE    = "SUBMISSION_CREATE";
    public static final String REPORT_VIEW          = "REPORT_VIEW";
    public static final String COUNCIL_MANAGE       = "COUNCIL_MANAGE";
    public static final String ASSIGNMENT_MANAGE    = "ASSIGNMENT_MANAGE";
    public static final String EVALUATION_CREATE    = "EVALUATION_CREATE";
    public static final String RESULT_CALCULATE     = "RESULT_CALCULATE";
    public static final String RESULT_PUBLISH       = "RESULT_PUBLISH";
    public static final String RESULT_VIEW          = "RESULT_VIEW";
    public static final String NOTICE_VIEW          = "NOTICE_VIEW";
    public static final String NOTICE_MANAGE        = "NOTICE_MANAGE";
}