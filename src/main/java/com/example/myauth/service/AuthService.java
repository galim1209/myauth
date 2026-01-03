package com.example.myauth.service;

import com.example.myauth.dto.LoginRequest;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.dto.SignupRequest;
import com.example.myauth.dto.TokenRefreshResponse;
import com.example.myauth.entity.RefreshToken;
import com.example.myauth.entity.User;
import com.example.myauth.exception.AccountException;
import com.example.myauth.exception.DuplicateEmailException;
import com.example.myauth.exception.InvalidCredentialsException;
import com.example.myauth.exception.TokenException;
import com.example.myauth.repository.RefreshTokenRepository;
import com.example.myauth.repository.UserRepository;
import com.example.myauth.security.CustomUserDetails;
import com.example.myauth.security.CustomUserDetailsService;
import com.example.myauth.security.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final CustomUserDetailsService customUserDetailsService;
  private final AuthenticationManager authenticationManager;


  /**
   * 회원가입 처리
   * 성공 시 정상 반환, 실패 시 예외 던지기
   */
  @Transactional
  public void registerUser(SignupRequest signupRequest) {
    // 이메일을 정규화한다 (공백 제거, 소문자 변환)
    String normalizedEmail = signupRequest.getEmail().trim().toLowerCase();

    try {
      // signupRequest 정보를 이용하여 User Entity 인스턴스를 생성한다
      User user = User.builder()
          .email(normalizedEmail)  // 정규화된 이메일 사용
          .password(passwordEncoder.encode(signupRequest.getPassword()))
          .name(signupRequest.getUsername())
          .role(User.Role.ROLE_USER)
          .status(User.Status.ACTIVE)
          .isActive(true)
          .build();

      // DB에 저장한다 - unique constraint 위반 시 예외 발생
      userRepository.save(user);
      log.info("회원 가입 성공 : {}", user.getEmail());

    } catch (DataIntegrityViolationException e) {
      // unique constraint 위반 (이미 존재하는 이메일)
      log.warn("중복된 이메일로 가입 시도 : {}", normalizedEmail);
      throw new DuplicateEmailException("이미 가입된 이메일입니다.");
    }
  }

  /**
   * 로그인 처리
   * 성공 시 LoginResponse 반환, 실패 시 예외 던지기
   */
  @Transactional
  public LoginResponse login(@Valid LoginRequest loginRequest) {
    // 1️⃣ 이메일을 정규화한다 (회원가입과 동일하게 처리)
    String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();
    log.info("로그인 시도: {}", normalizedEmail);

    // 2️⃣ 사용자를 조회한다
    User user = userRepository.findByEmail(normalizedEmail)
        .orElseThrow(() -> {
          log.warn("존재하지 않는 이메일로 로그인 시도: {}", normalizedEmail);
          // 보안상 이유로 이메일이 틀렸는지 비밀번호가 틀렸는지 알려주지 않음
          return new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        });

    // 3️⃣ 비밀번호를 검증한다
    boolean isPasswordValid = passwordEncoder.matches(
        loginRequest.getPassword(),  // 입력된 평문 비밀번호
        user.getPassword()            // DB에 저장된 암호화된 비밀번호
    );

    if (!isPasswordValid) {
      log.warn("잘못된 비밀번호로 로그인 시도: {}", normalizedEmail);
      // 보안상 동일한 에러 메시지 사용
      throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    // 4️⃣ 계정 상태를 확인한다

    // 4-1. 활성화 여부 확인
    if (!user.getIsActive()) {
      log.warn("비활성화된 계정으로 로그인 시도: {}", normalizedEmail);
      throw new AccountException("비활성화된 계정입니다. 고객센터에 문의해주세요.");
    }

    // 4-2. 계정 상태 확인
    if (user.getStatus() != User.Status.ACTIVE) {
      log.warn("비정상 상태 계정으로 로그인 시도: {} (상태: {})", normalizedEmail, user.getStatus());

      // 상태에 따라 다른 메시지 반환
      String errorMessage = switch (user.getStatus()) {
        case SUSPENDED -> "정지된 계정입니다. 고객센터에 문의해주세요.";
        case DELETED -> "삭제된 계정입니다.";
        case INACTIVE -> "비활성화된 계정입니다. 고객센터에 문의해주세요.";
        case PENDING_VERIFICATION -> "이메일 인증이 필요합니다.";
        default -> "로그인할 수 없는 계정 상태입니다.";
      };
      throw new AccountException(errorMessage);
    }

    // 5️⃣ JWT 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());

    log.info("JWT 토큰 생성 완료: {}", normalizedEmail);

    // 6️⃣ Refresh Token을 DB에 저장
    RefreshToken refreshTokenEntity = RefreshToken.builder()
        .token(refreshToken)
        .user(user)
        .expiresAt(LocalDateTime.ofInstant(
            jwtTokenProvider.getRefreshTokenExpiryDate().toInstant(),
            ZoneId.systemDefault()
        ))
        .build();

    refreshTokenRepository.save(refreshTokenEntity);
    log.info("Refresh Token DB 저장 완료: {}", normalizedEmail);

    // 7️⃣ 로그인 성공 응답 반환
    LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .role(user.getRole().name())
        .profileImage(user.getProfileImage())
        .build();

    log.info("로그인 성공: {}", normalizedEmail);
    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .user(userInfo)
        .build();
  }

  /**
   * 로그인 처리 (AuthenticationManager 사용 버전)
   * Spring Security의 표준 AuthenticationManager를 사용하여 로그인을 처리
   * - 사용자 조회, 비밀번호 검증, 계정 상태 확인을 자동으로 처리
   * 성공 시 LoginResponse 반환, 실패 시 예외 던지기
   */
  @Transactional
  public LoginResponse loginEx(@Valid LoginRequest loginRequest) {
    // 1️⃣ 이메일을 정규화한다
    String normalizedEmail = loginRequest.getEmail().trim().toLowerCase();
    log.info("로그인 시도 (loginEx): {}", normalizedEmail);

    // 2️⃣ AuthenticationManager를 통해 인증 처리
    // Spring Security가 자동으로:
    // - CustomUserDetailsService를 통해 사용자 조회
    // - PasswordEncoder를 통해 비밀번호 검증
    // - UserDetails의 계정 상태 확인 (enabled, accountNonLocked 등)
    Authentication authentication;
    try {
      authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.getPassword())
      );
    } catch (AuthenticationException e) {
      // 인증 실패: 사용자 없음, 비밀번호 불일치, 계정 비활성화 등
      log.warn("로그인 실패 (loginEx): {} - {}", normalizedEmail, e.getMessage());
      throw new InvalidCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    // 3️⃣ 인증 성공 시 User 엔티티 추출
    CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
    User user = customUserDetails.getUser();

    // 4️⃣ JWT 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
    log.info("JWT 토큰 생성 완료 (loginEx): {}", normalizedEmail);

    // 5️⃣ Refresh Token을 DB에 저장
    RefreshToken refreshTokenEntity = RefreshToken.builder()
        .token(refreshToken)
        .user(user)
        .expiresAt(LocalDateTime.ofInstant(
            jwtTokenProvider.getRefreshTokenExpiryDate().toInstant(),
            ZoneId.systemDefault()
        ))
        .build();

    refreshTokenRepository.save(refreshTokenEntity);
    log.info("Refresh Token DB 저장 완료 (loginEx): {}", normalizedEmail);

    // 6️⃣ 로그인 성공 응답 반환
    LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .role(user.getRole().name())
        .build();

    log.info("로그인 성공 (loginEx): {}", normalizedEmail);
    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .user(userInfo)
        .build();
  }

  /**
   * Refresh Token으로 Access Token 갱신
   * Refresh Token의 유효성을 검증하고 새로운 Access Token을 발급
   * 성공 시 TokenRefreshResponse 반환, 실패 시 예외 던지기
   */
  @Transactional(readOnly = true)
  public TokenRefreshResponse refreshAccessToken(String refreshToken) {
    log.info("Access Token 갱신 요청");

    // 1️⃣ Refresh Token 검증
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      log.warn("유효하지 않은 Refresh Token");
      throw new TokenException("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
    }

    // 2️⃣ Refresh Token에서 이메일 추출
    String email = jwtTokenProvider.getEmailFromToken(refreshToken);
    log.debug("Refresh Token에서 추출한 이메일: {}", email);

    // 3️⃣ DB에 해당 Refresh Token이 존재하는지 확인
    RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
        .orElseThrow(() -> {
          log.warn("DB에 존재하지 않는 Refresh Token");
          return new TokenException("유효하지 않은 Refresh Token입니다. 다시 로그인해주세요.");
        });

    // 4️⃣ Refresh Token이 만료되었는지 확인
    if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
      log.warn("만료된 Refresh Token: {}", email);
      throw new TokenException("Refresh Token이 만료되었습니다. 다시 로그인해주세요.");
    }

    // 5️⃣ 사용자 조회
    User user = refreshTokenEntity.getUser();
    if (user == null || !user.getIsActive() || user.getStatus() != User.Status.ACTIVE) {
      log.warn("비활성화된 사용자: {}", email);
      throw new AccountException("비활성화된 계정입니다. 고객센터에 문의해주세요.");
    }

    // 6️⃣ 새 Access Token 생성
    String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());
    log.info("새 Access Token 발급 성공: {}", email);

    return TokenRefreshResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(null)  // Refresh Token Rotation 미사용
        .build();
  }
}
