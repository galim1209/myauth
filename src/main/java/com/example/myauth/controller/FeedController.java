package com.example.myauth.controller;

import com.example.myauth.dto.ApiResponse;
import com.example.myauth.dto.post.PostListResponse;
import com.example.myauth.entity.User;
import com.example.myauth.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 피드 컨트롤러
 * 사용자 피드(타임라인) 관련 API 엔드포인트 제공
 *
 * 【API 목록】
 * - GET /api/feed             : 홈 피드 (팔로잉 게시글)
 * - GET /api/feed/explore     : 탐색 피드 (공개 게시글)
 * - GET /api/feed/popular     : 인기 피드 (좋아요 순)
 * - GET /api/feed/recommended : 추천 피드
 */
@Slf4j
@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

  private final FeedService feedService;

  // ===== 홈 피드 =====

  /**
   * 홈 피드 조회 (팔로잉 사용자의 게시글)
   *
   * GET /api/feed?page=0&size=10
   *
   * 【설명】
   * 로그인 사용자가 팔로우하는 사용자들의 게시글을 최신순으로 조회
   *
   * 【쿼리 파라미터】
   * - page: 페이지 번호 (0부터 시작, 기본값 0)
   * - size: 페이지 크기 (기본값 10, 최대 50)
   * - includeMyPosts: 내 게시글 포함 여부 (기본값 false)
   *
   * 【응답 예시】
   * {
   *   "success": true,
   *   "message": "홈 피드 조회 성공",
   *   "data": {
   *     "content": [
   *       { "id": 1, "content": "...", "author": {...}, ... }
   *     ],
   *     "totalElements": 100,
   *     "totalPages": 10
   *   }
   * }
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<PostListResponse>>> getHomeFeed(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "false") boolean includeMyPosts
  ) {
    log.info("홈 피드 조회 요청 - userId: {}", user.getId());

    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<PostListResponse> feed;

    if (includeMyPosts) {
      feed = feedService.getHomeFeedWithMyPosts(user.getId(), pageable);
    } else {
      feed = feedService.getHomeFeed(user.getId(), pageable);
    }

    return ResponseEntity.ok(ApiResponse.success("홈 피드 조회 성공", feed));
  }

  // ===== 탐색 피드 =====

  /**
   * 탐색 피드 조회 (공개 게시글, 최신순)
   *
   * GET /api/feed/explore?page=0&size=10
   *
   * 【설명】
   * 모든 공개 게시글을 최신순으로 조회
   * 비로그인 사용자도 접근 가능
   */
  @GetMapping("/explore")
  public ResponseEntity<ApiResponse<Page<PostListResponse>>> getExploreFeed(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("탐색 피드 조회 요청");

    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<PostListResponse> feed = feedService.getExploreFeed(pageable);

    return ResponseEntity.ok(ApiResponse.success("탐색 피드 조회 성공", feed));
  }

  // ===== 인기 피드 =====

  /**
   * 인기 피드 조회 (좋아요 순)
   *
   * GET /api/feed/popular?page=0&size=10
   *
   * 【설명】
   * 좋아요 수가 많은 순으로 공개 게시글 조회
   */
  @GetMapping("/popular")
  public ResponseEntity<ApiResponse<Page<PostListResponse>>> getPopularFeed(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("인기 피드 조회 요청");

    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<PostListResponse> feed = feedService.getExploreFeedByPopularity(pageable);

    return ResponseEntity.ok(ApiResponse.success("인기 피드 조회 성공", feed));
  }

  /**
   * 조회수 순 피드 조회
   *
   * GET /api/feed/views?page=0&size=10
   */
  @GetMapping("/views")
  public ResponseEntity<ApiResponse<Page<PostListResponse>>> getViewsFeed(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("조회수 피드 조회 요청");

    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<PostListResponse> feed = feedService.getExploreFeedByViews(pageable);

    return ResponseEntity.ok(ApiResponse.success("조회수 피드 조회 성공", feed));
  }

  // ===== 추천 피드 =====

  /**
   * 추천 피드 조회
   *
   * GET /api/feed/recommended?page=0&size=10
   *
   * 【설명】
   * 사용자에게 맞춤형으로 추천되는 게시글 조회
   * 현재는 팔로우하지 않는 사용자의 인기 게시글 반환
   */
  @GetMapping("/recommended")
  public ResponseEntity<ApiResponse<Page<PostListResponse>>> getRecommendedFeed(
      @AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    log.info("추천 피드 조회 요청 - userId: {}", user.getId());

    if (size > 50) size = 50;

    Pageable pageable = PageRequest.of(page, size);
    Page<PostListResponse> feed = feedService.getRecommendedFeed(user.getId(), pageable);

    return ResponseEntity.ok(ApiResponse.success("추천 피드 조회 성공", feed));
  }
}
