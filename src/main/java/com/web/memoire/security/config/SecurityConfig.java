package com.web.memoire.security.config;

import com.web.memoire.security.filter.JWTFilter;
import com.web.memoire.security.filter.LoginFilter;
import com.web.memoire.security.handler.CustomLogoutHandler;
import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.security.model.service.CustomUserDetailsService;
import com.web.memoire.security.handler.CustomAuthenticationSuccessHandler; // ✅ 추가
import com.web.memoire.security.oauth2.CustomOAuth2UserService; // ✅ 추가
import com.web.memoire.user.jpa.repository.SocialUserRepository;
import com.web.memoire.user.jpa.repository.UserRepository;
import com.web.memoire.user.model.service.UserService;
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
    private final UserService userService; // ✅ UserService 주입 추가

    public SecurityConfig(CustomUserDetailsService userDetailsService, JWTUtil jwtUtil, UserRepository userRepository, TokenService tokenService, UserService userService) { // ✅ 생성자 인자에 UserService 추가
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.userService = userService; // ✅ UserService 초기화
    }

    @Bean
    public CustomLogoutHandler customLogoutHandler(TokenService tokenService) {
        return new CustomLogoutHandler(tokenService,jwtUtil);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

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

    // ✅ CustomOAuth2UserService 빈 등록
    @Bean
    public CustomOAuth2UserService customOAuth2UserService(UserRepository userRepository, SocialUserRepository socialUserRepository) {
        return new CustomOAuth2UserService(userRepository, socialUserRepository);
    }

    // ✅ CustomAuthenticationSuccessHandler 빈 등록
    @Bean
    public CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler(JWTUtil jwtUtil, TokenService tokenService, UserRepository userRepository) {
        return new CustomAuthenticationSuccessHandler(jwtUtil, tokenService, userRepository);
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
            JWTUtil jwtUtil,
            UserRepository userRepository,
            TokenService tokenService,
            UserService userService, // LoginFilter에 전달하기 위함
            CustomOAuth2UserService customOAuth2UserService, // ✅ 추가
            CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler // ✅ 추가
    ) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/atelier/**").permitAll() // ✅ 아뜰리에꺼! 요거만 하나 추가!
                        .requestMatchers("/", "/**", "/favicon.ico", "/manifest.json", "/public/**", "/auth/**",
                                "/css/**", "/js/**").permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers("/upload_files/**").permitAll()
                        .requestMatchers("/*.png").permitAll()
                        .requestMatchers("/*.jpg").permitAll()
                        .requestMatchers("/login", "/reissue", "/user/signup","/user/idcheck", "/user/social", "/user/socialSignUp").permitAll() // ✅ /user/social, /user/socialSignUp 추가
                        .requestMatchers("/logout").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // JWTFilter 생성자 인자에 userDetailsService 추가 (수정)
                .addFilterBefore(new JWTFilter(jwtUtil, userDetailsService), UsernamePasswordAuthenticationFilter.class)
                // LoginFilter 생성자 인자는 이미 올바르게 설정되어 있습니다. (확인)
                .addFilterAt(new LoginFilter(authenticationManager, jwtUtil, userRepository, tokenService, userService),
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
                )
                // ✅ OAuth2 로그인 설정 추가
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint -> endpoint
                                .baseUri("/oauth2/authorization") // 클라이언트가 소셜 로그인 시작 시 요청할 기본 URI
                        )
                        .redirectionEndpoint(endpoint -> endpoint
                                .baseUri("/login/oauth2/code/*") // IdP로부터 인가 코드를 받는 콜백 URI
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService) // ✅ CustomOAuth2UserService 등록
                        )
                        .successHandler(customAuthenticationSuccessHandler) // ✅ CustomAuthenticationSuccessHandler 등록
                        .failureHandler((request, response, exception) -> { // ✅ 실패 핸들러 추가
                            log.error("OAuth2 Login Failed: {}", exception.getMessage(), exception);
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json; charset=utf-8");
                            response.getWriter().write("{\"error\":\"소셜 로그인 실패: " + exception.getMessage() + "\"}");
                        })
                );

        return http.build();
    }
}