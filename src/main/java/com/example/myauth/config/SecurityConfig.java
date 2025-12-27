package com.example.myauth.config;

import com.example.myauth.security.CustomLogoutHandler;
import com.example.myauth.security.CustomLogoutSuccessHandler;
import com.example.myauth.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * - JWT 기반 인증 사용 (세션 사용 안 함)
 * - 경로별 인증 규칙 설정
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CustomLogoutHandler customLogoutHandler;
  private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

  /**
   * 비밀번호 암호화에 사용할 PasswordEncoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Spring Security의 표준 인증 관리자
   * 사용자 인증을 처리하는 핵심 컴포넌트
   * - UserDetailsService를 통해 사용자 정보 로드
   * - PasswordEncoder를 통해 비밀번호 검증
   * - 계정 상태 확인 (활성화, 잠금, 만료 등)
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  /**
   * Spring Security 필터 체인 설정
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // 1️⃣ CSRF 비활성화 (JWT 사용 시 불필요)
        .csrf(AbstractHttpConfigurer::disable)

        // 2️⃣ 폼 로그인 비활성화
        .formLogin(AbstractHttpConfigurer::disable)

        // 3️⃣ HTTP Basic 인증 비활성화
        .httpBasic(AbstractHttpConfigurer::disable)

        // 4️⃣ 세션 사용 안 함 (JWT 사용)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // 5️⃣ 로그아웃 설정
        .logout(logout -> logout
            .logoutUrl("/logout")  // 로그아웃 URL
            .addLogoutHandler(customLogoutHandler)  // 커스텀 로그아웃 핸들러 (Refresh Token 삭제)
            .logoutSuccessHandler(customLogoutSuccessHandler)  // 커스텀 성공 핸들러 (JSON 응답)
            .permitAll()  // 로그아웃 URL은 인증 없이 접근 가능
        )

        // 6️⃣ 경로별 인증 규칙 설정
        .authorizeHttpRequests(auth ->
            auth
                // 인증 없이 접근 가능한 경로
                .requestMatchers("/health", "/signup", "/login", "/loginEx", "/refresh").permitAll()
                // 카카오 OAuth 로그인 경로 (인증 불필요)
                .requestMatchers("/auth/kakao/**").permitAll()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
        )
        .exceptionHandling(ex -> ex
            // 인증 실패 시 401 Unauthorized 반환 (기본 403 대신)
            .authenticationEntryPoint((request, response, authException) -> {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.setContentType("application/json;charset=UTF-8");
              response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\"}");
            })
            // 권한 부족시 403 Forbidden
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.setStatus(HttpServletResponse.SC_FORBIDDEN);
              response.setContentType("application/json;charset=UTF-8");
              response.getWriter().write("{\"error\":\"Access Denied\", \"message\":\"권한이 없습니다.\"}");
            })
        )

        // JWT 인증 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 실행)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}

