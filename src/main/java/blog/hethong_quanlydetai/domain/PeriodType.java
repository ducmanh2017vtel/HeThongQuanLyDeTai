package blog.hethong_quanlydetai.domain;

public enum PeriodType {
    MON_HOC, NCKH, TLCN, KLTN;

    /** Có yêu cầu hạn chót GVPB nộp điểm về Khoa hay không (A6). */
    public boolean requiresReviewDeadline() {
        return this == TLCN || this == KLTN;
    }

    /** Có yêu cầu ngày báo cáo hội đồng hay không. */
    public boolean requiresCouncilReportDate() {
        return this == KLTN;
    }
}