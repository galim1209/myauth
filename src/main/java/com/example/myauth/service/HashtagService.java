package com.example.myauth.service;

import com.example.myauth.dto.hashtag.HashtagResponse;
import com.example.myauth.dto.hashtag.TrendingHashtagResponse;
import com.example.myauth.dto.post.PostListResponse;
import com.example.myauth.entity.Hashtag;
import com.example.myauth.entity.Post;
import com.example.myauth.entity.PostHashtag;
import com.example.myauth.exception.HashtagNotFoundException;
import com.example.myauth.repository.HashtagRepository;
import com.example.myauth.repository.PostHashtagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 해시태그 서비스
 * 해시태그 추출, 관리, 검색 비즈니스 로직
 *
 * 【주요 기능】
 * - 본문에서 해시태그 추출
 * - 해시태그 생성/조회
 * - 게시글-해시태그 연결 관리
 * - 인기 해시태그 조회
 * - 해시태그로 게시글 검색
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HashtagService {

  private final HashtagRepository hashtagRepository;
  private final PostHashtagRepository postHashtagRepository;

  /**
   * 해시태그 패턴: #한글영문숫자_
   * 예: #맛집, #서울여행, #daily_life
   */
  private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([\\w가-힣]+)");

  // ===== 해시태그 추출 =====

  /**
   * 본문에서 해시태그 추출
   * 예: "오늘 #맛집 탐방! #서울맛집 #데이트" → ["맛집", "서울맛집", "데이트"]
   *
   * @param content 게시글/댓글 본문
   * @return 추출된 해시태그 이름 목록 (중복 제거, 소문자)
   */
  public List<String> extractHashtags(String content) {
    if (content == null || content.isBlank()) {
      return Collections.emptyList();
    }

    List<String> hashtags = new ArrayList<>();
    Matcher matcher = HASHTAG_PATTERN.matcher(content);

    while (matcher.find()) {
      // 해시태그 이름 소문자로 정규화 (대소문자 구분 없이 동일 취급)
      String hashtag = matcher.group(1).toLowerCase();
      // 최대 길이 제한 (100자)
      if (hashtag.length() <= 100) {
        hashtags.add(hashtag);
      }
    }

    // 중복 제거
    return hashtags.stream().distinct().collect(Collectors.toList());
  }

  // ===== 해시태그 조회/생성 =====

  /**
   * 해시태그 조회 또는 생성
   * 이미 존재하면 조회, 없으면 새로 생성
   *
   * @param name 해시태그 이름
   * @return 해시태그 엔티티
   */
  @Transactional
  public Hashtag getOrCreateHashtag(String name) {
    String normalizedName = name.toLowerCase().trim();

    return hashtagRepository.findByName(normalizedName)
        .orElseGet(() -> {
          log.info("새 해시태그 생성: #{}", normalizedName);
          Hashtag hashtag = Hashtag.builder()
              .name(normalizedName)
              .postCount(0)
              .build();
          return hashtagRepository.save(hashtag);
        });
  }

  /**
   * 여러 해시태그 조회 또는 생성
   *
   * @param names 해시태그 이름 목록
   * @return 해시태그 엔티티 목록
   */
  @Transactional
  public List<Hashtag> getOrCreateHashtags(List<String> names) {
    if (names == null || names.isEmpty()) {
      return Collections.emptyList();
    }

    return names.stream()
        .map(this::getOrCreateHashtag)
        .collect(Collectors.toList());
  }

  // ===== 게시글-해시태그 연결 =====

  /**
   * 게시글에 해시태그 연결
   * 본문에서 해시태그를 추출하여 자동으로 연결
   *
   * @param post 게시글 엔티티
   * @param content 게시글 본문
   * @return 연결된 해시태그 목록
   */
  @Transactional
  public List<Hashtag> linkHashtagsToPost(Post post, String content) {
    // 1. 본문에서 해시태그 추출
    List<String> hashtagNames = extractHashtags(content);

    if (hashtagNames.isEmpty()) {
      return Collections.emptyList();
    }

    log.info("게시글 {} 에 해시태그 연결: {}", post.getId(), hashtagNames);

    // 2. 해시태그 조회 또는 생성
    List<Hashtag> hashtags = getOrCreateHashtags(hashtagNames);

    // 3. 게시글-해시태그 연결
    for (Hashtag hashtag : hashtags) {
      // 이미 연결되어 있는지 확인
      if (!postHashtagRepository.existsByPostIdAndHashtagId(post.getId(), hashtag.getId())) {
        PostHashtag postHashtag = PostHashtag.create(post, hashtag);
        postHashtagRepository.save(postHashtag);
      }
    }

    return hashtags;
  }

  /**
   * 게시글의 해시태그 업데이트
   * 기존 연결을 모두 삭제하고 새로 연결
   *
   * @param post 게시글 엔티티
   * @param content 게시글 본문
   * @return 연결된 해시태그 목록
   */
  @Transactional
  public List<Hashtag> updatePostHashtags(Post post, String content) {
    // 1. 기존 해시태그 연결의 hashtag ID 목록 조회
    List<Long> oldHashtagIds = postHashtagRepository.findHashtagIdsByPostId(post.getId());

    // 2. 기존 연결 삭제
    postHashtagRepository.deleteByPostId(post.getId());

    // 3. 기존 해시태그의 postCount 감소
    for (Long hashtagId : oldHashtagIds) {
      hashtagRepository.decrementPostCount(hashtagId);
    }

    // 4. 새로운 해시태그 연결
    return linkHashtagsToPost(post, content);
  }

  /**
   * 게시글의 해시태그 연결 해제
   * 게시글 삭제 시 호출
   *
   * @param postId 게시글 ID
   */
  @Transactional
  public void unlinkHashtagsFromPost(Long postId) {
    log.info("게시글 {} 의 해시태그 연결 해제", postId);

    // 1. 연결된 해시태그 ID 목록 조회
    List<Long> hashtagIds = postHashtagRepository.findHashtagIdsByPostId(postId);

    // 2. 연결 삭제
    postHashtagRepository.deleteByPostId(postId);

    // 3. 해시태그 postCount 감소
    for (Long hashtagId : hashtagIds) {
      hashtagRepository.decrementPostCount(hashtagId);
    }
  }

  // ===== 인기 해시태그 =====

  /**
   * 인기 해시태그 조회
   *
   * @param pageable 페이지 정보
   * @return 인기 해시태그 페이지
   */
  @Transactional(readOnly = true)
  public Page<TrendingHashtagResponse> getTrendingHashtags(Pageable pageable) {
    log.info("인기 해시태그 조회");

    Page<Hashtag> hashtags = hashtagRepository.findTrendingHashtags(pageable);
    return hashtags.map(TrendingHashtagResponse::from);
  }

  /**
   * 상위 N개 인기 해시태그 조회
   *
   * @param limit 조회할 개수
   * @return 인기 해시태그 목록
   */
  @Transactional(readOnly = true)
  public List<TrendingHashtagResponse> getTopTrendingHashtags(int limit) {
    log.info("상위 {} 개 인기 해시태그 조회", limit);

    List<Hashtag> hashtags = hashtagRepository.findTopTrendingHashtags(limit);
    return hashtags.stream()
        .map(TrendingHashtagResponse::from)
        .collect(Collectors.toList());
  }

  // ===== 해시태그로 게시글 검색 =====

  /**
   * 해시태그로 게시글 검색
   *
   * @param hashtagName 해시태그 이름 (# 제외)
   * @param pageable 페이지 정보
   * @return 게시글 페이지
   */
  @Transactional(readOnly = true)
  public Page<PostListResponse> getPostsByHashtag(String hashtagName, Pageable pageable) {
    String normalizedName = hashtagName.toLowerCase().trim();
    log.info("해시태그로 게시글 검색: #{}", normalizedName);

    // 해시태그 존재 확인
    Hashtag hashtag = hashtagRepository.findByName(normalizedName)
        .orElseThrow(() -> new HashtagNotFoundException(normalizedName));

    Page<Post> posts = postHashtagRepository.findPostsByHashtagId(hashtag.getId(), pageable);
    return posts.map(PostListResponse::from);
  }

  // ===== 해시태그 검색 =====

  /**
   * 해시태그 이름으로 검색
   *
   * @param keyword 검색 키워드
   * @param pageable 페이지 정보
   * @return 해시태그 검색 결과 페이지
   */
  @Transactional(readOnly = true)
  public Page<HashtagResponse> searchHashtags(String keyword, Pageable pageable) {
    log.info("해시태그 검색: {}", keyword);

    Page<Hashtag> hashtags = hashtagRepository.searchByName(keyword, pageable);
    return hashtags.map(HashtagResponse::from);
  }

  // ===== 해시태그 정보 조회 =====

  /**
   * 해시태그 정보 조회
   *
   * @param name 해시태그 이름
   * @return 해시태그 응답
   */
  @Transactional(readOnly = true)
  public HashtagResponse getHashtag(String name) {
    String normalizedName = name.toLowerCase().trim();

    Hashtag hashtag = hashtagRepository.findByName(normalizedName)
        .orElseThrow(() -> new HashtagNotFoundException(normalizedName));

    return HashtagResponse.from(hashtag);
  }
}
