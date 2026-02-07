package com.example.myauth.dto.hashtag;

import com.example.myauth.entity.Hashtag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인기 해시태그 응답 DTO
 * 트렌딩/인기 해시태그 목록 조회 시 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendingHashtagResponse {

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
   */
  private String hashtag;

  /**
   * 이 해시태그를 사용한 게시글 수
   */
  private Integer postCount;

  /**
   * 순위 (선택적, 조회 시 설정)
   */
  private Integer rank;

  /**
   * Entity → DTO 변환
   *
   * @param hashtag 해시태그 엔티티
   * @return 인기 해시태그 응답 DTO
   */
  public static TrendingHashtagResponse from(Hashtag hashtag) {
    return TrendingHashtagResponse.builder()
        .id(hashtag.getId())
        .name(hashtag.getName())
        .hashtag("#" + hashtag.getName())
        .postCount(hashtag.getPostCount())
        .build();
  }

  /**
   * Entity → DTO 변환 (순위 포함)
   *
   * @param hashtag 해시태그 엔티티
   * @param rank 순위
   * @return 인기 해시태그 응답 DTO
   */
  public static TrendingHashtagResponse from(Hashtag hashtag, int rank) {
    return TrendingHashtagResponse.builder()
        .id(hashtag.getId())
        .name(hashtag.getName())
        .hashtag("#" + hashtag.getName())
        .postCount(hashtag.getPostCount())
        .rank(rank)
        .build();
  }
}
