package com.example.myauth.dto.hashtag;

import com.example.myauth.entity.Hashtag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 해시태그 응답 DTO
 * 해시태그 정보 조회 시 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagResponse {

  /**
   * 해시태그 ID
   */
  private Long id;

  /**
   * 해시태그 이름 (# 제외)
   */
  private String name;

  /**
   * 해시태그 문자열 (# 포함)
   * 예: "#맛집", "#여행"
   */
  private String hashtag;

  /**
   * 이 해시태그를 사용한 게시글 수
   */
  private Integer postCount;

  /**
   * 생성일시
   */
  private LocalDateTime createdAt;

  /**
   * Entity → DTO 변환
   *
   * @param hashtag 해시태그 엔티티
   * @return 해시태그 응답 DTO
   */
  public static HashtagResponse from(Hashtag hashtag) {
    return HashtagResponse.builder()
        .id(hashtag.getId())
        .name(hashtag.getName())
        .hashtag("#" + hashtag.getName())
        .postCount(hashtag.getPostCount())
        .createdAt(hashtag.getCreatedAt())
        .build();
  }
}
