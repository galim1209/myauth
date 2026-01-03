package com.example.myauth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 성공 시 반환하는 응답 DTO
 * ApiResponse의 data 필드에 담겨서 반환됨
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
  /**
   * Access Token (짧은 만료 시간, API 요청 시 사용)
   */
  private String accessToken;

  /**
   * Refresh Token (긴 만료 시간, Access Token 갱신 시 사용)
   */
  private String refreshToken;

  /**
   * 사용자 정보
   */
  private UserInfo user;

  /**
   * 사용자 기본 정보
   */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UserInfo {
    private Long id;
    private String email;
    private String name;
    private String role;
    private String profileImage;
  }
}