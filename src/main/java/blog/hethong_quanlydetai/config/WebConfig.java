package blog.hethong_quanlydetai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Mặc định tiếng Việt cho toàn bộ ứng dụng. */
    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(new Locale("vi", "VN"));
    }
}