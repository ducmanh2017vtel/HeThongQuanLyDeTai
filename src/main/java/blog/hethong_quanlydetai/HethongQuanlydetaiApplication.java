package blog.hethong_quanlydetai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Điểm vào ứng dụng Hệ thống quản lý đề tài sinh viên.
 */
@SpringBootApplication
@EnableAsync
public class HethongQuanlydetaiApplication {

    public static void main(String[] args) {
        // Đảm bảo múi giờ JVM là Asia/Ho_Chi_Minh ngay từ khi khởi động
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SpringApplication.run(HethongQuanlydetaiApplication.class, args);
    }
}