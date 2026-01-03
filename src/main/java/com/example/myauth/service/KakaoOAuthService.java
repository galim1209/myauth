package com.example.myauth.service;

import com.example.myauth.config.KakaoOAuthProperties;
import com.example.myauth.dto.kakao.KakaoOAuthDto;
import com.example.myauth.dto.LoginResponse;
import com.example.myauth.entity.RefreshToken;
import com.example.myauth.entity.User;
import com.example.myauth.repository.RefreshTokenRepository;
import com.example.myauth.repository.UserRepository;
import com.example.myauth.security.JwtTokenProvider;
//import com.tools.jackson.databind.ObjectMapper;  // Jackson 3 패키지 (Spring Boot 4.0.0)
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * 카카오 OAuth 로그인 서비스
 * 카카오 API를 통한 소셜 로그인 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoOAuthService {

  private final KakaoOAuthProperties kakaoProperties;
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final ObjectMapper objectMapper;  // JacksonConfig에서 주입받음
  private final RestClient restClient = RestClient.create();

  /**
   * 카카오 인가 코드 요청 URL 생성
   * 사용자를 카카오 로그인 페이지로 리다이렉트하기 위한 URL
   *
   * @return 카카오 인가 코드 요청 URL
   */
  public String getAuthorizationUrl() {
    String url = UriComponentsBuilder
        .fromUriString(kakaoProperties.getAuthorizationUri())
        .queryParam("client_id", kakaoProperties.getClientId())
        .queryParam("redirect_uri", kakaoProperties.getRedirectUri())
        .queryParam("response_type", "code")
        .build()
        .toUriString();

    log.info("카카오 인가 URL 생성: {}", url);
    return url;
  }

  /**
   * Authorization Code로 카카오 Access Token 요청
   *
   * @param code 카카오 인가 코드
   * @return 카카오 토큰 응답 DTO
   */
  public KakaoOAuthDto.TokenResponse getAccessToken(String code) {
    log.info("카카오 Access Token 요청 시작 - code: {}", code);

    // 요청 파라미터 구성
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", kakaoProperties.getClientId());
    params.add("client_secret", kakaoProperties.getClientSecret());
    params.add("redirect_uri", kakaoProperties.getRedirectUri());
    params.add("code", code);

    // ====== 📍 URL 및 요청 정보 출력 ======
    String tokenUrl = kakaoProperties.getTokenUri();
    log.info("========================================");
    log.info("카카오 토큰 API 호출 URL: {}", tokenUrl);
    log.info("요청 파라미터: grant_type={}, client_id={}, redirect_uri={}, code={}",
        params.getFirst("grant_type"),
        params.getFirst("client_id"),
        params.getFirst("redirect_uri"),
        code);
    log.info("========================================");

    // 카카오 토큰 API 호출
    KakaoOAuthDto.TokenResponse tokenResponse = restClient.post()
        .uri(tokenUrl)
        .contentType(MediaType.parseMediaType("application/x-www-form-urlencoded;charset=utf-8"))  // 카카오 문서 명시
        .body(params)
        .retrieve()
        .body(KakaoOAuthDto.TokenResponse.class);

    // ====== 📍 응답 JSON 출력 ======
    try {
      String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tokenResponse);
      log.info("========================================");
      log.info("카카오 토큰 API 응답 JSON:\n{}", responseJson);
      log.info("========================================");
    } catch (Exception e) {
      log.error("JSON 변환 실패: {}", e.getMessage());
    }

    log.info("카카오 Access Token 발급 성공");
    return tokenResponse;
  }

  /**
   * 카카오 Access Token으로 사용자 정보 조회
   *
   * @param accessToken 카카오 액세스 토큰
   * @return 카카오 사용자 정보 응답 DTO
   */
  public KakaoOAuthDto.UserInfoResponse getUserInfo(String accessToken) {
    log.info("카카오 사용자 정보 조회 시작");

    // ====== 📍 URL 및 요청 정보 출력 ======
    String userInfoUrl = kakaoProperties.getUserInfoUri();
    log.info("========================================");
    log.info("카카오 사용자 정보 API 호출 URL: {}", userInfoUrl);
    log.info("========================================");

    // 카카오 사용자 정보 API 호출
    KakaoOAuthDto.UserInfoResponse userInfo = restClient.get()
        .uri(userInfoUrl)
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .body(KakaoOAuthDto.UserInfoResponse.class);

    // ====== 📍 응답 JSON 출력 ======
    try {
      String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userInfo);
      log.info("========================================");
      log.info("카카오 사용자 정보 API 응답 JSON:\n{}", responseJson);
      log.info("========================================");
    } catch (Exception e) {
      log.error("JSON 변환 실패: {}", e.getMessage());
    }

    log.info("카카오 사용자 정보 조회 성공 - 카카오 ID: {}, 닉네임: {}",
        userInfo.getId(),
        userInfo.getKakaoAccount().getProfile().getNickname());

    return userInfo;
  }

  /**
   * 카카오 사용자 정보로 로그인 처리
   * 1. 기존 회원이면 로그인
   * 2. 신규 회원이면 자동 회원가입 후 로그인
   *
   * @param kakaoUserInfo 카카오 사용자 정보
   * @return 로그인 응답 DTO (JWT 포함)
   */
  @Transactional
  public LoginResponse processKakaoLogin(KakaoOAuthDto.UserInfoResponse kakaoUserInfo) {
    String providerId = String.valueOf(kakaoUserInfo.getId());
    String email = kakaoUserInfo.getKakaoAccount().getEmail();
    String nickname = kakaoUserInfo.getKakaoAccount().getProfile().getNickname();
    String profileImage = kakaoUserInfo.getKakaoAccount().getProfile().getProfileImageUrl();

    log.info("카카오 로그인 처리 시작 - 카카오 ID: {}, 이메일: {}, 닉네임: {}",
        providerId, email, nickname);

    // ✅ 이메일 필수 검증 (카카오 개발자 콘솔에서 필수 동의 항목으로 설정되어야 함)
    if (email == null || email.isBlank()) {
      log.error("카카오 로그인 실패 - 이메일이 제공되지 않음. 카카오 개발자 콘솔에서 이메일을 필수 동의 항목으로 설정하세요.");
      throw new IllegalArgumentException("카카오 계정의 이메일 정보가 필요합니다. 카카오 로그인 시 이메일 제공에 동의해주세요.");
    }

    // 1️⃣ 카카오 ID로 기존 회원 조회
    Optional<User> existingUser = userRepository.findByProviderAndProviderId("KAKAO", providerId);

    User user;
    if (existingUser.isPresent()) {
      // 기존 회원 - 로그인 처리
      user = existingUser.get();
      log.info("기존 카카오 회원 로그인: {}", user.getEmail());

      // 프로필 정보 업데이트 (닉네임, 프로필 이미지가 변경되었을 수 있음)
      user.setName(nickname);
      user.setProfileImage(profileImage);
      userRepository.save(user);

    } else {
      // 신규 회원 - 자동 회원가입
      log.info("신규 카카오 회원 가입 처리 - 이메일: {}, 닉네임: {}", email, nickname);

      user = User.builder()
          .email(email)  // 이메일 필수 (위에서 검증 완료)
          .name(nickname)
          .password(null)  // OAuth 로그인은 비밀번호 불필요
          .provider("KAKAO")
          .providerId(providerId)
          .profileImage(profileImage)
          .role(User.Role.ROLE_USER)
          .status(User.Status.ACTIVE)
          .isActive(true)
          .isSuperUser(false)
          .failedLoginAttempts(0)
          .build();

      userRepository.save(user);
      log.info("신규 카카오 회원 가입 완료 - ID: {}, 이메일: {}", user.getId(), user.getEmail());
    }

    // 2️⃣ JWT 토큰 생성
    String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId());
    String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
    log.info("JWT 토큰 생성 완료 - User ID: {}", user.getId());

    // 3️⃣ Refresh Token DB 저장
    RefreshToken refreshTokenEntity = RefreshToken.builder()
        .token(refreshToken)
        .user(user)
        .expiresAt(LocalDateTime.ofInstant(
            jwtTokenProvider.getRefreshTokenExpiryDate().toInstant(),
            ZoneId.systemDefault()
        ))
        .build();

    refreshTokenRepository.save(refreshTokenEntity);
    log.info("Refresh Token DB 저장 완료");

    // 4️⃣ 로그인 응답 생성
    LoginResponse.UserInfo userInfo = LoginResponse.UserInfo.builder()
        .id(user.getId())
        .email(user.getEmail())
        .name(user.getName())
        .role(user.getRole().name())
        .profileImage(user.getProfileImage())
        .build();

    return LoginResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .user(userInfo)
        .build();
  }
}
