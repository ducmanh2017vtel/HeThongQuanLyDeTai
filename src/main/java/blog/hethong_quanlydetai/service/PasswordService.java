package blog.hethong_quanlydetai.service;

import blog.hethong_quanlydetai.audit.AuditLogger;
import blog.hethong_quanlydetai.domain.User;
import blog.hethong_quanlydetai.dto.ChangePasswordForm;
import blog.hethong_quanlydetai.exception.BusinessException;
import blog.hethong_quanlydetai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Đổi mật khẩu tài khoản cá nhân. */
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    @Transactional
    public void changePassword(Long userId, ChangePasswordForm form) {
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu xác nhận không khớp");
        }
        if (form.getNewPassword().equals(form.getOldPassword())) {
            throw new BusinessException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        validateComplexity(form.getNewPassword());

        User u = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Tài khoản không tồn tại"));
        if (!passwordEncoder.matches(form.getOldPassword(), u.getPassword())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        u.setPassword(passwordEncoder.encode(form.getNewPassword()));
        userRepository.save(u);
        auditLogger.log("CHANGE_PASSWORD", "User", u.getId(),
                u.getUsername() + " đổi mật khẩu");
    }

    /** Tối thiểu 8 ký tự, có chữ và số (mục 5.1). */
    private void validateComplexity(String pw) {
        if (pw == null || pw.length() < 8) {
            throw new BusinessException("Mật khẩu phải có ít nhất 8 ký tự");
        }
        boolean hasLetter = pw.chars().anyMatch(Character::isLetter);
        boolean hasDigit  = pw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            throw new BusinessException("Mật khẩu phải có cả chữ và số");
        }
    }
}