package blog.hethong_quanlydetai.exception;

/** Lỗi nghiệp vụ cần thông báo tiếng Việt rõ ràng cho người dùng. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}