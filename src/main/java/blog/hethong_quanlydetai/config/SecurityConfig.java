package blog.hethong_quanlydetai.config;

import blog.hethong_quanlydetai.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.io.IOException;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final LoginAttemptService loginAttemptService;

    @Value("${app.security.login.max-fail}")
    private int maxFail;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // DelegatingPasswordEncoder: prefix {bcrypt}/{noop}/{pbkdf2}...
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new org.springframework.security.authentication.ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF: bật cho mọi form POST (mục 5.1 §1)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**", "/logout")  // API REST có thể dùng bearer riêng; logout dùng POST có CSRF
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            // Header bảo mật mặc định của Spring Security
            .headers(headers -> headers
                .contentTypeOptions(c -> {})
                .frameOptions(f -> f.deny())
                .httpStrictTransportSecurity(h -> h.includeSubDomains(true).maxAgeInSeconds(31536000))
                .referrerPolicy(r -> r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
            )
            // Session cố định khi đăng nhập (mục 5.1)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login",
                    "/css/**",
                    "/js/**",
                    "/webjars/**",
                    "/error",
                    "/favicon.ico"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler(authenticationSuccessHandler())
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((req, res, e) -> {
                    try {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType("text/html;charset=UTF-8");
                        res.getWriter().write(
                            "<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\">" +
                            "<title>403 - Không có quyền</title>" +
                            "<link rel=\"stylesheet\" href=\"/css/theme.css?v=3\">" +
                            "<link rel=\"stylesheet\" href=\"/css/layout.css?v=3\">" +
                            "<link rel=\"stylesheet\" href=\"/css/components.css?v=3\">" +
                            "</head><body><div class=\"error-page\">" +
                            "<h1>403</h1>" +
                            "<p class=\"msg\">Bạn không có quyền truy cập trang này.</p>" +
                            "<a class=\"btn btn-primary\" href=\"/dashboard\">Về Tổng quan</a>" +
                            "</div></body></html>");
                    } catch (java.io.IOException io) {
                        throw new org.springframework.security.core.AuthenticationException(
                                "Lỗi xử lý 403", io) {};
                    }
                })
                .authenticationEntryPoint((req, res, e) -> {
                    try {
                        res.sendRedirect("/login");
                    } catch (java.io.IOException io) {
                        throw new org.springframework.security.core.AuthenticationException(
                                "Lỗi chuyển hướng đăng nhập", io) {};
                    }
                })
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            try {
                String username = authentication.getName();
                loginAttemptService.loginSucceeded(username);
                response.sendRedirect("/dashboard");
            } catch (java.io.IOException e) {
                throw new org.springframework.security.authentication.AuthenticationServiceException(
                        "Lỗi chuyển hướng sau đăng nhập", e);
            }
        };
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler handler = new SimpleUrlAuthenticationFailureHandler("/login?error") {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, jakarta.servlet.ServletException {
                String username = request.getParameter("username");
                if (username != null && !username.isBlank()) {
                    loginAttemptService.loginFailed(username);
                }
                super.onAuthenticationFailure(request, response, exception);
            }
        };
        return handler;
    }
}