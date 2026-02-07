package com.example.myauth.exception;

/**
 * 해시태그를 찾을 수 없을 때 발생하는 예외
 */
public class HashtagNotFoundException extends RuntimeException {

  /**
   * 해시태그 이름으로 예외 생성
   *
   * @param name 해시태그 이름
   */
  public HashtagNotFoundException(String name) {
    super("해시태그를 찾을 수 없습니다: #" + name);
  }

  /**
   * 해시태그 ID로 예외 생성
   *
   * @param id 해시태그 ID
   */
  public HashtagNotFoundException(Long id) {
    super("해시태그를 찾을 수 없습니다. (ID: " + id + ")");
  }
}
