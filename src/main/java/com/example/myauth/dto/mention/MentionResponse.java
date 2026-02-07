package com.example.myauth.dto.mention;

import com.example.myauth.entity.Mention;
import com.example.myauth.entity.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 멘션 응답 DTO
 * 나를 멘션한 게시글/댓글 목록 조회 시 반환
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionResponse {

  /**
   * 멘션 ID
   */
  private Long id;

  /**
   * 멘션 대상 유형 (POST/COMMENT)
   */
  private TargetType targetType;

  /**
   * 멘션 대상 ID (게시글 ID 또는 댓글 ID)
   */
  private Long targetId;

  /**
   * 멘션된 일시
   */
  private LocalDateTime createdAt;

  /**
   * Entity → DTO 변환
   *
   * @param mention 멘션 엔티티
   * @return 멘션 응답 DTO
   */
  public static MentionResponse from(Mention mention) {
    return MentionResponse.builder()
        .id(mention.getId())
        .targetType(mention.getTargetType())
        .targetId(mention.getTargetId())
        .createdAt(mention.getCreatedAt())
        .build();
  }
}
