package com.example.myauth.repository;

import com.example.myauth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 정보를 관리하는 Repository
 * Spring Data JPA가 자동으로 구현을 생성한다
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  /**
   * 이메일로 사용자를 조회한다
   * @param email 조회할 이메일
   * @return 사용자 정보 (Optional)
   */
  Optional<User> findByEmail(String email);

  /**
   * 이메일이 이미 존재하는지 확인한다
   * @param email 확인할 이메일
   * @return 존재하면 true, 아니면 false
   */
  boolean existsByEmail(String email);

  /**
   * OAuth 제공자와 제공자 ID로 사용자를 조회한다 (카카오, 구글 등)
   * @param provider OAuth 제공자 (KAKAO, GOOGLE 등)
   * @param providerId OAuth 제공자의 사용자 고유 ID
   * @return 사용자 정보 (Optional)
   */
  Optional<User> findByProviderAndProviderId(String provider, String providerId);

  /**
   * 이름으로 사용자를 조회한다 (멘션 기능용)
   * @param name 사용자 이름
   * @return 사용자 정보 (Optional)
   */
  Optional<User> findByName(String name);
}
