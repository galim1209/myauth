package com.example.myauth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 게시글-해시태그 연결 엔티티
 * 게시글과 해시태그의 N:M 관계를 연결하는 중간 테이블
 * 한 게시글에 여러 해시태그, 한 해시태그에 여러 게시글
 *
 * 【테이블 정보】
 * - 테이블명: post_hashtags
 * - 주요 기능: 게시글-해시태그 N:M 관계 매핑
 * - 복합 기본키: (post_id, hashtag_id)
 *
 * 【연관 관계】
 * - Post: N:1 (여러 연결이 한 게시글에 속함)
 * - Hashtag: N:1 (여러 연결이 한 해시태그에 속함)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_hashtags", indexes = {
    // 특정 해시태그의 게시글 목록 조회용 인덱스
    @Index(name = "idx_hashtag_id", columnList = "hashtag_id")
})
@IdClass(PostHashtag.PostHashtagId.class)  // 복합 기본키 사용
public class PostHashtag {

  /**
   * 복합 기본키 클래스
   * post_id + hashtag_id 조합이 기본키
   */
  @Embeddable
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class PostHashtagId implements Serializable {
    private Long post;
    private Long hashtag;
  }

  /**
   * 소속 게시글 - Post 엔티티와 N:1 관계
   * 게시글 삭제 시 연결 정보 삭제 (해시태그 자체는 유지)
   */
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  /**
   * 연결된 해시태그 - Hashtag 엔티티와 N:1 관계
   * 해시태그 삭제 시 연결 정보 삭제
   */
  @Id
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hashtag_id", nullable = false)
  private Hashtag hashtag;

  /**
   * 연결 생성 일시 (자동 설정)
   */
  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // ===== 팩토리 메서드 =====

  /**
   * 게시글-해시태그 연결 생성
   * 주의: 양방향 관계 설정은 명시적 저장 후 별도로 처리
   * (영속성 컨텍스트 충돌 방지)
   */
  public static PostHashtag create(Post post, Hashtag hashtag) {
    PostHashtag postHashtag = PostHashtag.builder()
        .post(post)
        .hashtag(hashtag)
        .build();

    // 해시태그 사용 카운트 증가
    hashtag.incrementPostCount();

    return postHashtag;
  }
}