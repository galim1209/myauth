package com.example.myauth.service;

import com.example.myauth.dto.post.PostListResponse;
import com.example.myauth.entity.Post;
import com.example.myauth.entity.Visibility;
import com.example.myauth.repository.FollowRepository;
import com.example.myauth.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드 서비스
 * 사용자 피드(타임라인) 관련 비즈니스 로직
 *
 * 【주요 기능】
 * - 홈 피드: 팔로잉 사용자의 게시글
 * - 탐색 피드: 공개 게시글 (인기순/최신순)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

  private final PostRepository postRepository;
  private final FollowRepository followRepository;

  // ===== 홈 피드 =====

  /**
   * 홈 피드 조회 (팔로잉 사용자의 게시글)
   * 로그인 사용자가 팔로우하는 사람들의 게시글을 최신순으로 조회
   *
   * @param userId 로그인 사용자 ID
   * @param pageable 페이지 정보
   * @return 피드 게시글 페이지
   */
  @Transactional(readOnly = true)
  public Page<PostListResponse> getHomeFeed(Long userId, Pageable pageable) {
    log.info("홈 피드 조회 - userId: {}", userId);

    // 팔로잉 사용자의 게시글 조회 (공개 또는 팔로워 전용)
    Page<Post> posts = postRepository.findHomeFeed(userId, pageable);

    return posts.map(PostListResponse::from);
  }

  /**
   * 홈 피드 조회 (팔로잉 + 본인 게시글 포함)
   *
   * @param userId 로그인 사용자 ID
   * @param pageable 페이지 정보
   * @return 피드 게시글 페이지
   */
  @Transactional(readOnly = true)
  public Page<PostListResponse> getHomeFeedWithMyPosts(Long userId, Pageable pageable) {
    log.info("홈 피드 조회 (본인 포함) - userId: {}", userId);

    Page<Post> posts = postRepository.findHomeFeedWithMyPosts(userId, pageable);

    return posts.map(PostListResponse::from);
  }

  // ===== 탐색 피드 =====

  /**
   * 탐색 피드 조회 (공개 게시글, 최신순)
   * 모든 공개 게시글을 최신순으로 조회
   *
   * @param pageable 페이지 정보
   * @return 탐색 피드 페이지
   */
  @Transactional(readOnly = true)
  public Page<PostListResponse> getExploreFeed(Pageable pageable) {
    log.info("탐색 피드 조회 (최신순)");

    Page<Post> posts = postRepository.findPublicPostsOrderByCreatedAt(pageable);

    return posts.map(PostListResponse::from);
  }

  /**
   * 탐색 피드 조회 (공개 게시글, 인기순)
   * 좋아요 수가 많은 순으로 조회
   *
   * @param pageable 페이지 정보
   * @return 탐색 피드 페이지
   */
  @Transactional(readOnly = true)
  public Page<PostListResponse> getExploreFeedByPopularity(Pageable pageable) {
    log.info("탐색 피드 조회 (인기순)");

    Page<Post> posts = postRepository.findPublicPostsOrderByLikeCount(pageable);

    return posts.map(PostListResponse::from);
  }

  /**
   * 탐색 피드 조회 (공개 게시글, 조회수순)
   *
   * @param pageable 페이지 정보
   * @return 탐색 피드 페이지
   */
  @Transactional(readOnly = true)
  public Page<PostListResponse> getExploreFeedByViews(Pageable pageable) {
    log.info("탐색 피드 조회 (조회수순)");

    Page<Post> posts = postRepository.findPublicPostsOrderByViewCount(pageable);

    return posts.map(PostListResponse::from);
  }

  // ===== 추천 피드 (간단한 버전) =====

  /**
   * 추천 피드 조회
   * 현재는 간단하게 인기 게시글을 반환
   * 향후 사용자 관심사, 상호작용 패턴 기반 추천으로 확장 가능
   *
   * @param userId 로그인 사용자 ID
   * @param pageable 페이지 정보
   * @return 추천 피드 페이지
   */
  @Transactional(readOnly = true)
  public Page<PostListResponse> getRecommendedFeed(Long userId, Pageable pageable) {
    log.info("추천 피드 조회 - userId: {}", userId);

    // 현재는 팔로우하지 않는 사용자의 인기 게시글 반환
    Page<Post> posts = postRepository.findRecommendedPosts(userId, pageable);

    return posts.map(PostListResponse::from);
  }
}
