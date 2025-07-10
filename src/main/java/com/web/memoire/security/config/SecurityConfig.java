package com.web.memoire.security.config;

import com.web.memoire.security.filter.JWTFilter;
import com.web.memoire.security.filter.LoginFilter;
import com.web.memoire.security.handler.CustomLogoutHandler;
import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.security.model.service.CustomUserDetailsService;
import com.web.memoire.user.jpa.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final CustomUserDetailsService userDetailsService;
    private final JWTUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public SecurityConfig(CustomUserDetailsService userDetailsService, JWTUtil jwtUtil, UserRepository userRepository, TokenService tokenService) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    @Bean
    public CustomLogoutHandler customLogoutHandler(TokenService tokenService) {
        return new CustomLogoutHandler(tokenService,jwtUtil);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 인증 (Authentication) 관리자를 스프링부트 컨테이너에 Bean 으로 등록해야 함
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("token-expired", "Authorization", "RefreshToken")
                .allowCredentials(true);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager,
            CustomLogoutHandler customLogoutHandler,
            // JWTFilter와 LoginFilter에 필요한 의존성들을 Bean 메서드의 인자로 주입받습니다.
            JWTUtil jwtUtil, // JWTFilter와 LoginFilter에 필요
            UserRepository userRepository, // LoginFilter에 필요
            TokenService tokenService // LoginFilter에 필요
            // CustomUserDetailsService는 이미 SecurityConfig의 생성자에서 주입받아 필드로 가지고 있으므로, 직접 사용 가능
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/**", "/favicon.ico", "/manifest.json", "/public/**", "/auth/**",
                                "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/*.png").permitAll()
                        .requestMatchers("/login", "/reissue", "/user/signup","/user/idcheck").permitAll()
                        .requestMatchers("/logout").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // JWTFilter 생성자 인자에 userDetailsService 추가 (수정)
                .addFilterBefore(new JWTFilter(jwtUtil, userDetailsService), UsernamePasswordAuthenticationFilter.class)
                // LoginFilter 생성자 인자는 이미 올바르게 설정되어 있습니다. (확인)
                .addFilterAt(new LoginFilter(authenticationManager, jwtUtil, userRepository, tokenService),
                        UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .addLogoutHandler(customLogoutHandler)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write("로그아웃 성공");
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }
}
