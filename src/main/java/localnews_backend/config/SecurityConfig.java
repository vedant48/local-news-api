package localnews_backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    // ✅ Disable default Spring Security login
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll() // allow auth APIs
                        .anyRequest().permitAll() // allow all (we control via JwtFilter)
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    // ✅ Register JWT filter manually
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> jwtFilterBean() {
        FilterRegistrationBean<OncePerRequestFilter> registrationBean =
                new FilterRegistrationBean<>();

        registrationBean.setFilter(jwtFilter);
        registrationBean.addUrlPatterns("/*");

        return registrationBean;
    }
}