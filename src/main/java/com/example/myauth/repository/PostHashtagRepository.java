package com.example.myauth.repository;

import com.example.myauth.entity.Post;
import com.example.myauth.entity.PostHashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 게시글-해시태그 연결 리포지토리
 * 게시글과 해시태그 간의 N:M 관계 관리
 *
 * 【주요 기능】
 * - 게시글-해시태그 연결 관리
 * - 해시태그별 게시글 조회
 * - 게시글의 해시태그 목록 조회
 */
@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, PostHashtag.PostHashtagId> {

  // ===== 게시글-해시태그 연결 조회 =====

  /**
   * 게시글의 해시태그 연결 목록 조회
   *
   * @param postId 게시글 ID
   * @return 해시태그 연결 목록
   */
  @Query("SELECT ph FROM PostHashtag ph JOIN FETCH ph.hashtag WHERE ph.post.id = :postId")
  List<PostHashtag> findByPostIdWithHashtag(@Param("postId") Long postId);

  /**
   * 특정 해시태그를 사용한 게시글 목록 조회 (최신순)
   *
   * @param hashtagId 해시태그 ID
   * @param pageable 페이지 정보
   * @return 게시글 페이지
   */
  @Query("SELECT ph.post FROM PostHashtag ph " +
      "WHERE ph.hashtag.id = :hashtagId " +
      "AND ph.post.isDeleted = false " +
      "AND ph.post.visibility = 'PUBLIC' " +
      "ORDER BY ph.post.createdAt DESC")
  Page<Post> findPostsByHashtagId(@Param("hashtagId") Long hashtagId, Pageable pageable);

  /**
   * 해시태그 이름으로 게시글 목록 조회
   *
   * @param hashtagName 해시태그 이름 (# 제외)
   * @param pageable 페이지 정보
   * @return 게시글 페이지
   */
  @Query("SELECT ph.post FROM PostHashtag ph " +
      "WHERE ph.hashtag.name = :hashtagName " +
      "AND ph.post.isDeleted = false " +
      "AND ph.post.visibility = 'PUBLIC' " +
      "ORDER BY ph.post.createdAt DESC")
  Page<Post> findPostsByHashtagName(@Param("hashtagName") String hashtagName, Pageable pageable);

  // ===== 게시글-해시태그 연결 확인 =====

  /**
   * 게시글과 해시태그 연결 존재 여부 확인
   *
   * @param postId 게시글 ID
   * @param hashtagId 해시태그 ID
   * @return 연결 존재 여부
   */
  boolean existsByPostIdAndHashtagId(Long postId, Long hashtagId);

  // ===== 게시글-해시태그 연결 삭제 =====

  /**
   * 게시글의 모든 해시태그 연결 삭제
   *
   * @param postId 게시글 ID
   */
  @Modifying
  @Query("DELETE FROM PostHashtag ph WHERE ph.post.id = :postId")
  void deleteByPostId(@Param("postId") Long postId);

  /**
   * 특정 게시글-해시태그 연결 삭제
   *
   * @param postId 게시글 ID
   * @param hashtagId 해시태그 ID
   */
  @Modifying
  @Query("DELETE FROM PostHashtag ph WHERE ph.post.id = :postId AND ph.hashtag.id = :hashtagId")
  void deleteByPostIdAndHashtagId(@Param("postId") Long postId, @Param("hashtagId") Long hashtagId);

  // ===== 통계 =====

  /**
   * 특정 해시태그를 사용한 게시글 수 조회
   *
   * @param hashtagId 해시태그 ID
   * @return 게시글 수
   */
  @Query("SELECT COUNT(ph) FROM PostHashtag ph " +
      "WHERE ph.hashtag.id = :hashtagId AND ph.post.isDeleted = false")
  long countActivePostsByHashtagId(@Param("hashtagId") Long hashtagId);

  /**
   * 게시글의 해시태그 ID 목록 조회
   *
   * @param postId 게시글 ID
   * @return 해시태그 ID 목록
   */
  @Query("SELECT ph.hashtag.id FROM PostHashtag ph WHERE ph.post.id = :postId")
  List<Long> findHashtagIdsByPostId(@Param("postId") Long postId);
}
