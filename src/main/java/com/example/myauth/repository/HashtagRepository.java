package com.example.myauth.repository;

import com.example.myauth.entity.Hashtag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 해시태그 리포지토리
 * 해시태그 관리 및 검색 기능 제공
 *
 * 【주요 기능】
 * - 해시태그 조회/생성
 * - 인기 해시태그 조회
 * - 해시태그 검색
 */
@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

  // ===== 해시태그 조회 =====

  /**
   * 이름으로 해시태그 조회
   *
   * @param name 해시태그 이름 (# 제외)
   * @return 해시태그 Optional
   */
  Optional<Hashtag> findByName(String name);

  /**
   * 이름으로 해시태그 존재 여부 확인
   *
   * @param name 해시태그 이름
   * @return 존재 여부
   */
  boolean existsByName(String name);

  /**
   * 여러 이름으로 해시태그 목록 조회
   *
   * @param names 해시태그 이름 목록
   * @return 해시태그 목록
   */
  List<Hashtag> findByNameIn(List<String> names);

  // ===== 인기 해시태그 =====

  /**
   * 인기 해시태그 조회 (게시글 수 기준 내림차순)
   *
   * @param pageable 페이지 정보
   * @return 인기 해시태그 페이지
   */
  @Query("SELECT h FROM Hashtag h WHERE h.postCount > 0 ORDER BY h.postCount DESC")
  Page<Hashtag> findTrendingHashtags(Pageable pageable);

  /**
   * 상위 N개 인기 해시태그 조회
   *
   * @param limit 조회할 개수
   * @return 인기 해시태그 목록
   */
  @Query(value = "SELECT * FROM hashtags WHERE post_count > 0 ORDER BY post_count DESC LIMIT :limit",
      nativeQuery = true)
  List<Hashtag> findTopTrendingHashtags(@Param("limit") int limit);

  // ===== 해시태그 검색 =====

  /**
   * 해시태그 이름으로 검색 (부분 일치)
   *
   * @param keyword 검색 키워드
   * @param pageable 페이지 정보
   * @return 검색 결과 페이지
   */
  @Query("SELECT h FROM Hashtag h WHERE h.name LIKE %:keyword% ORDER BY h.postCount DESC")
  Page<Hashtag> searchByName(@Param("keyword") String keyword, Pageable pageable);

  // ===== 게시글 수 업데이트 =====

  /**
   * 게시글 수 증가
   *
   * @param hashtagId 해시태그 ID
   */
  @Modifying
  @Query("UPDATE Hashtag h SET h.postCount = h.postCount + 1 WHERE h.id = :hashtagId")
  void incrementPostCount(@Param("hashtagId") Long hashtagId);

  /**
   * 게시글 수 감소
   *
   * @param hashtagId 해시태그 ID
   */
  @Modifying
  @Query("UPDATE Hashtag h SET h.postCount = h.postCount - 1 WHERE h.id = :hashtagId AND h.postCount > 0")
  void decrementPostCount(@Param("hashtagId") Long hashtagId);
}
