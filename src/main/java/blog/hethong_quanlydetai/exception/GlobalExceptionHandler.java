package blog.hethong_quanlydetai.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex,
                                 RedirectAttributes redirectAttrs,
                                 HttpServletRequest req) {
        log.debug("Lỗi nghiệp vụ: {}", ex.getMessage());
        redirectAttrs.addFlashAttribute("flashError", ex.getMessage());
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegal(IllegalArgumentException ex,
                                RedirectAttributes redirectAttrs,
                                HttpServletRequest req) {
        redirectAttrs.addFlashAttribute("flashError", ex.getMessage());
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex,
                                      RedirectAttributes redirectAttrs,
                                      HttpServletRequest req) {
        log.warn("Lỗi ràng buộc dữ liệu: {}", ex.getMostSpecificCause().getMessage());
        redirectAttrs.addFlashAttribute("flashError",
                "Dữ liệu vi phạm ràng buộc (trùng khoá, khoá ngoại, hoặc CHECK). Vui lòng kiểm tra lại.");
        String referer = req.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     Model model,
                                     HttpServletResponse response) {
        model.addAttribute("status", 403);
        model.addAttribute("message", "Bạn không có quyền truy cập trang này.");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "error/403";
    }
}