package com.example.myauth.repository;

import com.example.myauth.entity.Mention;
import com.example.myauth.entity.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 멘션 리포지토리
 * @username 멘션 기록 관리
 *
 * 【주요 기능】
 * - 멘션 저장/삭제
 * - 사용자가 멘션된 게시글/댓글 조회
 * - 멘션 알림용 데이터 조회
 */
@Repository
public interface MentionRepository extends JpaRepository<Mention, Long> {

  // ===== 사용자별 멘션 조회 =====

  /**
   * 특정 사용자가 멘션된 목록 조회 (최신순)
   *
   * @param userId 멘션된 사용자 ID
   * @param pageable 페이지 정보
   * @return 멘션 페이지
   */
  Page<Mention> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

  /**
   * 특정 사용자가 게시글에서 멘션된 목록 조회
   *
   * @param userId 멘션된 사용자 ID
   * @param pageable 페이지 정보
   * @return 멘션 페이지
   */
  @Query("SELECT m FROM Mention m " +
      "WHERE m.user.id = :userId AND m.targetType = 'POST' " +
      "ORDER BY m.createdAt DESC")
  Page<Mention> findPostMentionsByUserId(@Param("userId") Long userId, Pageable pageable);

  /**
   * 특정 사용자가 댓글에서 멘션된 목록 조회
   *
   * @param userId 멘션된 사용자 ID
   * @param pageable 페이지 정보
   * @return 멘션 페이지
   */
  @Query("SELECT m FROM Mention m " +
      "WHERE m.user.id = :userId AND m.targetType = 'COMMENT' " +
      "ORDER BY m.createdAt DESC")
  Page<Mention> findCommentMentionsByUserId(@Param("userId") Long userId, Pageable pageable);

  // ===== 대상별 멘션 조회 =====

  /**
   * 특정 게시글/댓글의 멘션 목록 조회
   *
   * @param targetType 대상 유형 (POST/COMMENT)
   * @param targetId 대상 ID
   * @return 멘션 목록
   */
  List<Mention> findByTargetTypeAndTargetId(TargetType targetType, Long targetId);

  /**
   * 특정 게시글의 멘션된 사용자 ID 목록 조회
   *
   * @param postId 게시글 ID
   * @return 사용자 ID 목록
   */
  @Query("SELECT m.user.id FROM Mention m WHERE m.targetType = 'POST' AND m.targetId = :postId")
  List<Long> findMentionedUserIdsByPostId(@Param("postId") Long postId);

  /**
   * 특정 댓글의 멘션된 사용자 ID 목록 조회
   *
   * @param commentId 댓글 ID
   * @return 사용자 ID 목록
   */
  @Query("SELECT m.user.id FROM Mention m WHERE m.targetType = 'COMMENT' AND m.targetId = :commentId")
  List<Long> findMentionedUserIdsByCommentId(@Param("commentId") Long commentId);

  // ===== 멘션 존재 여부 확인 =====

  /**
   * 특정 대상에 특정 사용자가 멘션되었는지 확인
   *
   * @param userId 사용자 ID
   * @param targetType 대상 유형
   * @param targetId 대상 ID
   * @return 멘션 존재 여부
   */
  boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, TargetType targetType, Long targetId);

  // ===== 멘션 삭제 =====

  /**
   * 특정 게시글/댓글의 모든 멘션 삭제
   *
   * @param targetType 대상 유형
   * @param targetId 대상 ID
   */
  @Modifying
  @Query("DELETE FROM Mention m WHERE m.targetType = :targetType AND m.targetId = :targetId")
  void deleteByTargetTypeAndTargetId(@Param("targetType") TargetType targetType, @Param("targetId") Long targetId);

  /**
   * 특정 게시글의 모든 멘션 삭제
   *
   * @param postId 게시글 ID
   */
  @Modifying
  @Query("DELETE FROM Mention m WHERE m.targetType = 'POST' AND m.targetId = :postId")
  void deleteByPostId(@Param("postId") Long postId);

  /**
   * 특정 댓글의 모든 멘션 삭제
   *
   * @param commentId 댓글 ID
   */
  @Modifying
  @Query("DELETE FROM Mention m WHERE m.targetType = 'COMMENT' AND m.targetId = :commentId")
  void deleteByCommentId(@Param("commentId") Long commentId);

  // ===== 통계 =====

  /**
   * 특정 사용자가 멘션된 횟수 조회
   *
   * @param userId 사용자 ID
   * @return 멘션 횟수
   */
  long countByUserId(Long userId);
}
