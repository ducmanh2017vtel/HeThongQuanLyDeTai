package blog.hethong_quanlydetai;

import blog.hethong_quanlydetai.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm tra phân quyền ở GĐ2 với @WithMockUser.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessTest {

    @Autowired MockMvc mvc;

    @Test
    void unauthenticated_redirectedToLogin() throws Exception {
        mvc.perform(get("/admin/users"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(authorities = "USER_MANAGE")
    void userManage_canAccessUsersList() throws Exception {
        mvc.perform(get("/admin/users"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_GIANG_VIEN")
    void lecturer_cannotAccessUsersList() throws Exception {
        mvc.perform(get("/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_MANAGE")
    void roleManage_canAccessRolesList() throws Exception {
        mvc.perform(get("/admin/roles"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "PERMISSION_MANAGE")
    void permissionManage_canAccessPermissions() throws Exception {
        mvc.perform(get("/admin/permissions"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "anyuser", authorities = "NOTHING")
    void csrf_requiredOnPost() throws Exception {
        // POST thiếu CSRF token sẽ bị từ chối
        mvc.perform(post("/me/change-password")
                .param("oldPassword", "Demo@123")
                .param("newPassword", "NewPass123")
                .param("confirmPassword", "NewPass123"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "anyuser", authorities = "NOTHING")
    void csrf_okOnPost() throws Exception {
        mvc.perform(post("/me/change-password")
                .with(csrf())
                .param("oldPassword", "Demo@123")
                .param("newPassword", "NewPass123")
                .param("confirmPassword", "NewPass123"))
            // Không phải 403 (CSRF pass). Kết quả có thể 302 (validation fail)
            .andExpect(result -> {
                int status = result.getResponse().getStatus();
                if (status == 403) {
                    throw new AssertionError("CSRF OK mà vẫn 403");
                }
            });
    }
}