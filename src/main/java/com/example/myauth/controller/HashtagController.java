package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.hashtag.HashtagResponse;
import com.example.myauth.dto.hashtag.TrendingHashtagResponse;
import com.example.myauth.dto.post.PostListResponse;
import com.example.myauth.service.HashtagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 해시태그 컨트롤러
 * 해시태그 관련 API 엔드포인트 제공
 *
 * 【API 목록】
 * - GET /api/hashtags/trending         : 인기 해시태그 목록
 * - GET /api/hashtags/search           : 해시태그 검색
 * - GET /api/hashtags/{name}           : 해시태그 정보 조회
 * - GET /api/hashtags/{name}/posts     : 해시태그로 게시글 검색
 */
@Slf4j
@RestController
@RequestMapping("/api/hashtags")
@RequiredArgsConstructor
public class HashtagController {

  private final HashtagService hashtagService;

  // ===== 인기 해시태그 =====

  /**
   * 인기 해시태그 목록 조회
   *
   * GET /api/hashtags/trending?page=0&size=10
   *
   * 【쿼리 파라미터】
   * - page: 페이지 번호 (0부터 시작, 기본값 0)
   * - size: 페이지 크기 (기본값 10, 최대 50)
   *
   * 【응답 예시】
   * {
   *   "success": true,
   *   "data": {
   *     "content": [
   *       { "id": 1, "name": "맛집", "hashtag": "#맛집", "postCount": 1500 },
   *       { "id": 2, "name": "여행", "hashtag": "#여행", "postCount": 1200 }
   *     ]
   *   }
   * }
   */
  @GetMapping("/trending")
  public ResponseEntity<ApiResponse<Page<TrendingHashtagResponse>>> getTrendingHashtags(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("인기 해시태그 조회 요청");

    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<TrendingHashtagResponse> hashtags = hashtagService.getTrendingHashtags(pageable);

    return ResponseEntity.ok(ApiResponse.success("인기 해시태그 조회 성공", hashtags));
  }

  /**
   * 상위 N개 인기 해시태그 조회
   *
   * GET /api/hashtags/trending/top?limit=10
   */
  @GetMapping("/trending/top")
  public ResponseEntity<ApiResponse<List<TrendingHashtagResponse>>> getTopTrendingHashtags(
      @RequestParam(defaultValue = "10") int limit
  ) {
    log.info("상위 {} 개 인기 해시태그 조회 요청", limit);

    if (limit > 50) limit = 50;

    List<TrendingHashtagResponse> hashtags = hashtagService.getTopTrendingHashtags(limit);

    return ResponseEntity.ok(ApiResponse.success("인기 해시태그 조회 성공", hashtags));
  }

  // ===== 해시태그 검색 =====

  /**
   * 해시태그 검색
   *
   * GET /api/hashtags/search?keyword=맛&page=0&size=10
   *
   * 【쿼리 파라미터】
   * - keyword: 검색 키워드 (필수)
   * - page: 페이지 번호
   * - size: 페이지 크기
   */
  @GetMapping("/search")
  public ResponseEntity<ApiResponse<Page<HashtagResponse>>> searchHashtags(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("해시태그 검색 요청: {}", keyword);

    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<HashtagResponse> hashtags = hashtagService.searchHashtags(keyword, pageable);

    return ResponseEntity.ok(ApiResponse.success("해시태그 검색 성공", hashtags));
  }

  // ===== 해시태그 정보 조회 =====

  /**
   * 해시태그 정보 조회
   *
   * GET /api/hashtags/{name}
   *
   * 【경로 변수】
   * - name: 해시태그 이름 (# 제외)
   */
  @GetMapping("/{name}")
  public ResponseEntity<ApiResponse<HashtagResponse>> getHashtag(
      @PathVariable String name
  ) {
    log.info("해시태그 정보 조회: #{}", name);

    HashtagResponse hashtag = hashtagService.getHashtag(name);

    return ResponseEntity.ok(ApiResponse.success("해시태그 조회 성공", hashtag));
  }

  // ===== 해시태그로 게시글 검색 =====

  /**
   * 해시태그로 게시글 검색
   *
   * GET /api/hashtags/{name}/posts?page=0&size=10
   *
   * 【경로 변수】
   * - name: 해시태그 이름 (# 제외)
   *
   * 【쿼리 파라미터】
   * - page: 페이지 번호
   * - size: 페이지 크기
   */
  @GetMapping("/{name}/posts")
  public ResponseEntity<ApiResponse<Page<PostListResponse>>> getPostsByHashtag(
      @PathVariable String name,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("해시태그로 게시글 검색: #{}", name);

    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<PostListResponse> posts = hashtagService.getPostsByHashtag(name, pageable);

    return ResponseEntity.ok(ApiResponse.success("해시태그 게시글 조회 성공", posts));
  }
}
