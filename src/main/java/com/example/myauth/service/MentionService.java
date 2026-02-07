package com.example.myauth.service;

import com.example.myauth.dto.mention.MentionResponse;
import com.example.myauth.entity.Mention;
import com.example.myauth.entity.TargetType;
import com.example.myauth.entity.User;
import com.example.myauth.repository.MentionRepository;
import com.example.myauth.repository.UserRepository;
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
 * 멘션 서비스
 * @username 멘션 추출, 저장, 조회 비즈니스 로직
 *
 * 【주요 기능】
 * - 본문에서 멘션 추출
 * - 멘션 저장
 * - 사용자가 멘션된 게시글/댓글 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MentionService {

  private final MentionRepository mentionRepository;
  private final UserRepository userRepository;

  /**
   * 멘션 패턴: @사용자이름 (영문, 숫자, 언더스코어, 한글)
   * 예: @홍길동, @user123, @test_user
   */
  private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w가-힣]+)");

  // ===== 멘션 추출 =====

  /**
   * 본문에서 멘션된 사용자 이름 추출
   * 예: "@홍길동 님과 @김철수 님 안녕하세요!" → ["홍길동", "김철수"]
   *
   * @param content 게시글/댓글 본문
   * @return 멘션된 사용자 이름 목록 (중복 제거)
   */
  public List<String> extractMentions(String content) {
    if (content == null || content.isBlank()) {
      return Collections.emptyList();
    }

    List<String> mentions = new ArrayList<>();
    Matcher matcher = MENTION_PATTERN.matcher(content);

    while (matcher.find()) {
      String username = matcher.group(1);
      // 최대 길이 제한 (100자)
      if (username.length() <= 100) {
        mentions.add(username);
      }
    }

    // 중복 제거
    return mentions.stream().distinct().collect(Collectors.toList());
  }

  // ===== 멘션 처리 =====

  /**
   * 게시글의 멘션 처리
   * 본문에서 멘션을 추출하여 저장
   *
   * @param content 게시글 본문
   * @param postId 게시글 ID
   * @param authorId 작성자 ID (자기 자신 멘션 제외용)
   * @return 멘션된 사용자 목록
   */
  @Transactional
  public List<User> processPostMentions(String content, Long postId, Long authorId) {
    return processMentions(content, TargetType.POST, postId, authorId);
  }

  /**
   * 댓글의 멘션 처리
   * 본문에서 멘션을 추출하여 저장
   *
   * @param content 댓글 본문
   * @param commentId 댓글 ID
   * @param authorId 작성자 ID (자기 자신 멘션 제외용)
   * @return 멘션된 사용자 목록
   */
  @Transactional
  public List<User> processCommentMentions(String content, Long commentId, Long authorId) {
    return processMentions(content, TargetType.COMMENT, commentId, authorId);
  }

  /**
   * 멘션 처리 공통 로직
   *
   * @param content 본문
   * @param targetType 대상 유형 (POST/COMMENT)
   * @param targetId 대상 ID
   * @param authorId 작성자 ID
   * @return 멘션된 사용자 목록
   */
  @Transactional
  public List<User> processMentions(String content, TargetType targetType, Long targetId, Long authorId) {
    // 1. 본문에서 멘션 추출
    List<String> usernames = extractMentions(content);

    if (usernames.isEmpty()) {
      return Collections.emptyList();
    }

    log.info("{} {} 에 멘션 처리: {}", targetType, targetId, usernames);

    List<User> mentionedUsers = new ArrayList<>();

    // 2. 각 사용자 이름으로 사용자 조회 및 멘션 저장
    for (String username : usernames) {
      // 사용자 이름으로 조회 (이름 필드로 검색)
      userRepository.findByName(username).ifPresent(user -> {
        // 자기 자신 멘션 제외
        if (!user.getId().equals(authorId)) {
          // 중복 멘션 확인
          if (!mentionRepository.existsByUserIdAndTargetTypeAndTargetId(user.getId(), targetType, targetId)) {
            Mention mention;
            if (targetType == TargetType.POST) {
              mention = Mention.forPost(user, targetId);
            } else {
              mention = Mention.forComment(user, targetId);
            }
            mentionRepository.save(mention);
            mentionedUsers.add(user);

            log.info("멘션 저장: {} (userId: {}) in {} {}", username, user.getId(), targetType, targetId);
          }
        }
      });
    }

    return mentionedUsers;
  }

  // ===== 멘션 업데이트 =====

  /**
   * 게시글의 멘션 업데이트
   * 기존 멘션을 삭제하고 새로 처리
   *
   * @param content 게시글 본문
   * @param postId 게시글 ID
   * @param authorId 작성자 ID
   * @return 멘션된 사용자 목록
   */
  @Transactional
  public List<User> updatePostMentions(String content, Long postId, Long authorId) {
    // 기존 멘션 삭제
    mentionRepository.deleteByPostId(postId);
    // 새 멘션 처리
    return processPostMentions(content, postId, authorId);
  }

  /**
   * 댓글의 멘션 업데이트
   *
   * @param content 댓글 본문
   * @param commentId 댓글 ID
   * @param authorId 작성자 ID
   * @return 멘션된 사용자 목록
   */
  @Transactional
  public List<User> updateCommentMentions(String content, Long commentId, Long authorId) {
    // 기존 멘션 삭제
    mentionRepository.deleteByCommentId(commentId);
    // 새 멘션 처리
    return processCommentMentions(content, commentId, authorId);
  }

  // ===== 멘션 삭제 =====

  /**
   * 게시글의 모든 멘션 삭제
   *
   * @param postId 게시글 ID
   */
  @Transactional
  public void deletePostMentions(Long postId) {
    log.info("게시글 {} 의 멘션 삭제", postId);
    mentionRepository.deleteByPostId(postId);
  }

  /**
   * 댓글의 모든 멘션 삭제
   *
   * @param commentId 댓글 ID
   */
  @Transactional
  public void deleteCommentMentions(Long commentId) {
    log.info("댓글 {} 의 멘션 삭제", commentId);
    mentionRepository.deleteByCommentId(commentId);
  }

  // ===== 멘션 조회 =====

  /**
   * 나를 멘션한 목록 조회
   *
   * @param userId 사용자 ID
   * @param pageable 페이지 정보
   * @return 멘션 페이지
   */
  @Transactional(readOnly = true)
  public Page<MentionResponse> getMyMentions(Long userId, Pageable pageable) {
    log.info("멘션 목록 조회 - userId: {}", userId);

    Page<Mention> mentions = mentionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    return mentions.map(MentionResponse::from);
  }

  /**
   * 나를 게시글에서 멘션한 목록 조회
   *
   * @param userId 사용자 ID
   * @param pageable 페이지 정보
   * @return 멘션 페이지
   */
  @Transactional(readOnly = true)
  public Page<MentionResponse> getMyPostMentions(Long userId, Pageable pageable) {
    log.info("게시글 멘션 목록 조회 - userId: {}", userId);

    Page<Mention> mentions = mentionRepository.findPostMentionsByUserId(userId, pageable);
    return mentions.map(MentionResponse::from);
  }

  /**
   * 나를 댓글에서 멘션한 목록 조회
   *
   * @param userId 사용자 ID
   * @param pageable 페이지 정보
   * @return 멘션 페이지
   */
  @Transactional(readOnly = true)
  public Page<MentionResponse> getMyCommentMentions(Long userId, Pageable pageable) {
    log.info("댓글 멘션 목록 조회 - userId: {}", userId);

    Page<Mention> mentions = mentionRepository.findCommentMentionsByUserId(userId, pageable);
    return mentions.map(MentionResponse::from);
  }

  /**
   * 멘션 수 조회
   *
   * @param userId 사용자 ID
   * @return 멘션 수
   */
  @Transactional(readOnly = true)
  public long getMentionCount(Long userId) {
    return mentionRepository.countByUserId(userId);
  }
}
