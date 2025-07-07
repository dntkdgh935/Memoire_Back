package com.web.memoire.security.config;

import com.web.memoire.security.filter.JWTFilter;
import com.web.memoire.security.filter.LoginFilter;
import com.web.memoire.security.handler.CustomLogoutHandler;
import com.web.memoire.security.jwt.JWTUtil;
import com.web.memoire.security.jwt.model.service.TokenService;
import com.web.memoire.security.model.service.CustomUserDetailsService;
import com.web.memoire.user.jpa.repository.UserRepository;
import jakarta.persistence.Id;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.SecurityBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager, CustomLogoutHandler customLogoutHandler) throws Exception {
        //http.csrf(AbstractHttpConfigurer::disable);  // 아래 코드도 동일한 기능임
        //Spring Security 6 이상에서 사용하는 람다 기반 보안 설정 방식 사용함
        http
                .csrf(csrf -> csrf.disable())
                // CSRF는 사용자가 인증된 세션을 가진 상태에서 악성 요청이 전송되는 것을 방지하는 보안 기능
                //  CSRF(Cross Site Request Forgery, 교차 사이트 요청 위조) 보호 설정을 비활성화
                // 기본적으로 Spring Security는 POST, PUT, DELETE 요청에 대해 CSRF 토큰을 요구함
                // React, Vue 등 프론트엔드가 별도로 있고, JWT 토큰을 이용한 인증 방식일 때 비활성화함
                .cors(cors -> {})  // CORS 설정 활성화
                .formLogin(form -> form.disable())  //시큐리티가 제공하는 로그인 폼 사용 못하게 함
                .httpBasic(basic -> basic.disable())  //form 태그로 submit 해서 오는 요청은 사용 못하게 함
                //인증과 인가를 설정하는 부분
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 및 인증(/auth/**) 관련 요청은 인가에서 제외하기 위한 url 지정 (무조건 통과됨)
                        .requestMatchers("/", "/**", "/favicon.ico", "/manifest.json", "/public/**", "/auth/**",
                                "/css/**", "/js/**").permitAll()
                        // .png 파일은 인증없이 접근 허용함
                        .requestMatchers("/*.png").permitAll()
                        // 로그인, 토큰 재발급, 회원가입도 인증없이 접근 허용함
                        .requestMatchers("/login", "/reissue", "/signup").permitAll()
                        // 로그아웃은 인증된 사용자만 요청 가능 (인가 확인 필요)
                        .requestMatchers("/logout").authenticated()
                        // 관리자 전용 서비스인 경우 ROLE_ADMIN 권한 확인 필요함
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 나머지 모든 요청은 인증 확인 필요함 (로그인해야 요청할 수 있는 서비스들)
                        .anyRequest().authenticated()
                )
                // 모든 url 이 인가에서는 제외되었지만 (permitAll) JWTFilter 는 무조건 실행됨 => 토큰 검사함
                // 해결방법 : JWTFilter 안에 특정 url 에 대해 토큰검사 제외하는 기능 추가함
                .addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
                // 로그인 인증(Authentication) 은 인증 관리자(AuthenticationManager)가 관리해야 함
                .addFilterAt(new LoginFilter(authenticationManager, jwtUtil, userRepository, tokenService),
                        UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/logout")  // 로그아웃 요청 url 지정
                        .addLogoutHandler(customLogoutHandler)  // SecurityFilterChain 에 추가할 Handler 등록 : CustomLogoutHandler 등록
                        .logoutSuccessHandler((request, response, authentication) -> {
                            // 오버라이딩한 logout() 을 작동시키고 리턴된 성공정보를 클라이언트에게 내보냄
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.getWriter().write("로그아웃 성공");
                        })
                        .invalidateHttpSession(true)   // 세션 무효화
                        .clearAuthentication(true)  // 인증 정보 제거
                        .deleteCookies("JSESSIONID")  // 쿠키 제거
                );

        return http.build();
    }



}
