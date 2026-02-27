# MyAuth Backend API 완벽 가이드

프론트엔드 개발을 위한 백엔드 API 테스트 및 연동 가이드입니다.

## 목차

1. [서버 정보](#서버-정보)
2. [공통 규칙](#공통-규칙)
3. [인증 API](#인증-api)
4. [사용자/프로필 API](#사용자프로필-api)
5. [게시글 API](#게시글-api)
6. [댓글 API](#댓글-api)
7. [좋아요 API](#좋아요-api)
8. [팔로우 API](#팔로우-api)
9. [북마크 API](#북마크-api)
10. [피드 API](#피드-api)
11. [해시태그 API](#해시태그-api)
12. [이미지 업로드 API](#이미지-업로드-api)
13. [curl 테스트 예제](#curl-테스트-예제)

---

## 서버 정보

| 환경 | URL |
|------|-----|
| 개발 (로컬) | `http://localhost:9080` |
| 프로덕션 | `http://16.184.53.118:8080` |

---

## 공통 규칙

### 공통 응답 형식

모든 API는 다음과 같은 공통 응답 형식을 사용합니다:

```json
{
  "success": true,
  "message": "성공 메시지",
  "data": { ... }
}
```

### 에러 응답 형식

```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

### 인증 헤더

인증이 필요한 API는 다음 헤더를 포함해야 합니다:

```
Authorization: Bearer {accessToken}
```

### 페이징 파라미터

목록 조회 API는 다음 쿼리 파라미터를 지원합니다:

| 파라미터 | 기본값 | 최대값 | 설명 |
|----------|--------|--------|------|
| `page` | 0 | - | 페이지 번호 (0부터 시작) |
| `size` | 10 | 50 | 페이지 크기 |

### 페이징 응답 형식

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 10,
  "size": 10,
  "number": 0,
  "first": true,
  "last": false,
  "empty": false
}
```

---

## 인증 API

### 1. 헬스체크

서버 상태를 확인합니다.

```
GET /api/health
```

**응답:**
```json
{
  "success": true,
  "message": "Auth Service is running",
  "data": null
}
```

---

### 2. 회원가입

```
POST /api/signup
Content-Type: application/json
```

**요청:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "username": "홍길동"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | O | 이메일 (유효한 형식) |
| password | string | O | 비밀번호 (최소 8자) |
| username | string | O | 사용자 이름 |

**응답 (201 Created):**
```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": null
}
```

---

### 3. 로그인

```
POST /api/login
Content-Type: application/json
```

**요청:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답:**
```json
{
  "success": true,
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
    "user": {
      "id": 1,
      "email": "user@example.com",
      "name": "홍길동",
      "role": "USER",
      "profileImage": null
    }
  }
}
```

> **참고:** 웹 클라이언트의 경우 `refreshToken`은 HTTP-only 쿠키로 설정되며, 응답 바디에서는 `null`입니다.

---

### 4. 토큰 갱신

```
POST /api/refresh
Content-Type: application/json
```

**요청 (모바일):**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**응답:**
```json
{
  "success": true,
  "message": "Access Token이 갱신되었습니다",
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9..."
  }
}
```

---

### 5. 카카오 로그인

#### 5.1 로그인 시작 (리다이렉트)

```
GET /api/auth/kakao/login?redirectUrl={프론트엔드콜백URL}
```

| 파라미터 | 설명 |
|----------|------|
| redirectUrl | OAuth 완료 후 리다이렉트될 프론트엔드 URL |

**예시:**
```
http://localhost:9080/api/auth/kakao/login?redirectUrl=http://localhost:5173/auth/kakao/callback
```

> 이 API를 호출하면 카카오 로그인 페이지로 리다이렉트됩니다.

#### 5.2 콜백 처리 (자동)

카카오 인증 완료 후 백엔드가 자동으로 처리하며, 프론트엔드로 다음과 같이 리다이렉트됩니다:

**성공 시:**
```
{redirectUrl}#accessToken={토큰}&user={인코딩된사용자정보}
```

**실패 시:**
```
{redirectUrl}?error={에러메시지}
```

---

## 사용자/프로필 API

### 1. 현재 사용자 정보 조회

```
GET /api/user/me
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "message": "사용자 정보 조회 성공",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "profileImage": "http://...",
    "provider": "KAKAO",
    "role": "USER",
    "status": "ACTIVE",
    "isActive": true,
    "createdAt": "2025-01-01T10:00:00"
  }
}
```

---

### 2. 프로필 조회

```
GET /api/user/profile
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "message": "프로필 조회 성공",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "profileImage": "http://...",
    "provider": "KAKAO",
    "profileId": 1,
    "lastName": "홍",
    "firstName": "길동",
    "phoneNumber": "010-1234-5678",
    "country": 82,
    "address1": "서울시 강남구",
    "address2": "테헤란로 123",
    "birth": "1990-01-01T00:00:00",
    "bgImage": "http://...",
    "createdAt": "2025-01-01T10:00:00",
    "updatedAt": "2025-01-15T14:30:00"
  }
}
```

---

### 3. 프로필 수정

```
PUT /api/user/profile
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**요청:**
```json
{
  "name": "홍길동",
  "profileImage": "http://...",
  "lastName": "홍",
  "firstName": "길동",
  "phoneNumber": "010-1234-5678",
  "country": 82,
  "address1": "서울시 강남구",
  "address2": "테헤란로 123",
  "birth": "1990-01-01T00:00:00",
  "bgImage": "http://..."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| name | string | X | 닉네임 |
| profileImage | string | X | 프로필 이미지 URL |
| lastName | string | X | 성 |
| firstName | string | X | 이름 |
| phoneNumber | string | X | 전화번호 |
| country | number | X | 국가 코드 |
| address1 | string | X | 기본 주소 |
| address2 | string | X | 상세 주소 |
| birth | string | X | 생년월일 (ISO 8601) |
| bgImage | string | X | 배경 이미지 URL |

---

## 게시글 API

### 1. 게시글 작성 (텍스트만)

```
POST /api/posts
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**요청:**
```json
{
  "content": "오늘 맛있는 저녁 먹었어요! #맛집 #저녁 @친구",
  "visibility": "PUBLIC"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| content | string | O | 게시글 내용 (최대 5000자) |
| visibility | string | X | 공개 범위 (PUBLIC/PRIVATE/FOLLOWERS, 기본값: PUBLIC) |

**응답 (201 Created):**
```json
{
  "success": true,
  "message": "게시글이 작성되었습니다.",
  "data": {
    "id": 1,
    "content": "오늘 맛있는 저녁 먹었어요! #맛집 #저녁 @친구",
    "visibility": "PUBLIC",
    "likeCount": 0,
    "commentCount": 0,
    "viewCount": 0,
    "author": {
      "id": 1,
      "name": "홍길동",
      "profileImage": null
    },
    "images": [],
    "isLiked": false,
    "isBookmarked": false,
    "createdAt": "2025-01-24T10:30:00",
    "updatedAt": "2025-01-24T10:30:00"
  }
}
```

---

### 2. 게시글 작성 (이미지 포함)

```
POST /api/posts/with-images
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```

**요청:**
- `post` (JSON): 게시글 정보
- `images` (File[]): 이미지 파일들

**예시 (FormData):**
```javascript
const formData = new FormData();
formData.append('post', new Blob([JSON.stringify({
  content: "오늘 맛있는 저녁 먹었어요!",
  visibility: "PUBLIC"
})], { type: 'application/json' }));
formData.append('images', file1);
formData.append('images', file2);
```

---

### 3. 게시글 수정

```
PUT /api/posts/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**요청:**
```json
{
  "content": "수정된 게시글 내용",
  "visibility": "FOLLOWERS"
}
```

---

### 4. 게시글 삭제

```
DELETE /api/posts/{id}
Authorization: Bearer {accessToken}
```

---

### 5. 게시글 상세 조회

```
GET /api/posts/{id}
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "content": "게시글 내용...",
    "visibility": "PUBLIC",
    "likeCount": 42,
    "commentCount": 5,
    "viewCount": 100,
    "author": {
      "id": 1,
      "name": "홍길동",
      "profileImage": "http://..."
    },
    "images": [
      {
        "id": 1,
        "imageUrl": "http://...",
        "thumbnailUrl": "http://...",
        "sortOrder": 0
      }
    ],
    "isLiked": true,
    "isBookmarked": false,
    "createdAt": "2025-01-24T10:30:00",
    "updatedAt": "2025-01-24T10:30:00"
  }
}
```

---

### 6. 공개 게시글 목록

```
GET /api/posts?page=0&size=10
```

---

### 7. 내 게시글 목록

```
GET /api/posts/me?page=0&size=10
Authorization: Bearer {accessToken}
```

---

### 8. 특정 사용자 게시글 목록

```
GET /api/posts/user/{userId}?page=0&size=10
```

---

## 댓글 API

### 1. 댓글 작성

```
POST /api/posts/{postId}/comments
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**요청:**
```json
{
  "content": "좋은 게시글이네요!"
}
```

---

### 2. 대댓글 작성

```
POST /api/comments/{commentId}/replies
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**요청:**
```json
{
  "content": "저도 그렇게 생각해요!"
}
```

---

### 3. 댓글 수정

```
PUT /api/comments/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json
```

**요청:**
```json
{
  "content": "수정된 댓글 내용"
}
```

---

### 4. 댓글 삭제

```
DELETE /api/comments/{id}
Authorization: Bearer {accessToken}
```

---

### 5. 게시글 댓글 목록

```
GET /api/posts/{postId}/comments?page=0&size=20
Authorization: Bearer {accessToken} (선택)
```

**응답:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "content": "좋은 게시글이네요!",
        "author": {
          "id": 1,
          "name": "홍길동",
          "profileImage": null
        },
        "likeCount": 5,
        "replyCount": 2,
        "isLiked": false,
        "createdAt": "2025-01-24T11:00:00",
        "updatedAt": "2025-01-24T11:00:00"
      }
    ],
    "totalElements": 10
  }
}
```

---

### 6. 대댓글 목록

```
GET /api/comments/{commentId}/replies
Authorization: Bearer {accessToken} (선택)
```

---

## 좋아요 API

### 1. 게시글 좋아요

```
POST /api/posts/{postId}/like
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "message": "좋아요 완료",
  "data": {
    "targetType": "POST",
    "targetId": 1,
    "liked": true,
    "likeCount": 43
  }
}
```

---

### 2. 게시글 좋아요 취소

```
DELETE /api/posts/{postId}/like
Authorization: Bearer {accessToken}
```

---

### 3. 게시글 좋아요 사용자 목록

```
GET /api/posts/{postId}/likes?page=0&size=20
```

---

### 4. 댓글 좋아요

```
POST /api/comments/{commentId}/like
Authorization: Bearer {accessToken}
```

---

### 5. 댓글 좋아요 취소

```
DELETE /api/comments/{commentId}/like
Authorization: Bearer {accessToken}
```

---

## 팔로우 API

### 1. 팔로우

```
POST /api/users/{userId}/follow
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "message": "팔로우 완료",
  "data": {
    "userId": 2,
    "following": true,
    "followerCount": 100,
    "followingCount": 50
  }
}
```

---

### 2. 언팔로우

```
DELETE /api/users/{userId}/follow
Authorization: Bearer {accessToken}
```

---

### 3. 팔로워 목록

```
GET /api/users/{userId}/followers?page=0&size=20
Authorization: Bearer {accessToken} (선택)
```

**응답:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "홍길동",
        "profileImage": "http://...",
        "isFollowing": true
      }
    ]
  }
}
```

---

### 4. 팔로잉 목록

```
GET /api/users/{userId}/followings?page=0&size=20
Authorization: Bearer {accessToken} (선택)
```

---

### 5. 팔로워/팔로잉 수 조회

```
GET /api/users/{userId}/follow/count
```

**응답:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "followerCount": 100,
    "followingCount": 50
  }
}
```

---

### 6. 팔로우 여부 확인

```
GET /api/users/{userId}/follow/check
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "data": true
}
```

---

## 북마크 API

### 1. 북마크 추가

```
POST /api/posts/{postId}/bookmark
Authorization: Bearer {accessToken}
```

**응답:**
```json
{
  "success": true,
  "message": "북마크 완료",
  "data": {
    "postId": 1,
    "bookmarked": true
  }
}
```

---

### 2. 북마크 삭제

```
DELETE /api/posts/{postId}/bookmark
Authorization: Bearer {accessToken}
```

---

### 3. 내 북마크 목록

```
GET /api/me/bookmarks?page=0&size=10
Authorization: Bearer {accessToken}
```

---

### 4. 북마크 여부 확인

```
GET /api/posts/{postId}/bookmark/check
Authorization: Bearer {accessToken}
```

---

## 피드 API

### 1. 홈 피드 (팔로잉 게시글)

```
GET /api/feed?page=0&size=10&includeMyPosts=false
Authorization: Bearer {accessToken}
```

| 파라미터 | 기본값 | 설명 |
|----------|--------|------|
| page | 0 | 페이지 번호 |
| size | 10 | 페이지 크기 |
| includeMyPosts | false | 내 게시글 포함 여부 |

---

### 2. 탐색 피드 (공개 게시글)

```
GET /api/feed/explore?page=0&size=10
```

> 인증 불필요 - 비로그인 사용자도 접근 가능

---

### 3. 인기 피드 (좋아요 순)

```
GET /api/feed/popular?page=0&size=10
```

---

### 4. 조회수 피드

```
GET /api/feed/views?page=0&size=10
```

---

### 5. 추천 피드

```
GET /api/feed/recommended?page=0&size=10
Authorization: Bearer {accessToken}
```

---

## 해시태그 API

### 1. 인기 해시태그 (페이징)

```
GET /api/hashtags/trending?page=0&size=10
```

**응답:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "name": "맛집",
        "hashtag": "#맛집",
        "postCount": 1500,
        "rank": 1
      }
    ]
  }
}
```

---

### 2. 상위 N개 인기 해시태그

```
GET /api/hashtags/trending/top?limit=10
```

---

### 3. 해시태그 검색

```
GET /api/hashtags/search?keyword=맛&page=0&size=10
```

---

### 4. 해시태그 정보 조회

```
GET /api/hashtags/{name}
```

**예시:** `GET /api/hashtags/맛집`

---

### 5. 해시태그로 게시글 검색

```
GET /api/hashtags/{name}/posts?page=0&size=10
```

**예시:** `GET /api/hashtags/맛집/posts`

---

## 이미지 업로드 API

### 1. 이미지 업로드

```
POST /api/upload/image
Authorization: Bearer {accessToken}
Content-Type: multipart/form-data
```

**요청:**
- `file` (File): 이미지 파일

**예시 (JavaScript):**
```javascript
const formData = new FormData();
formData.append('file', imageFile);

const response = await fetch('/api/upload/image', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  body: formData
});
```

**응답:**
```json
{
  "success": true,
  "message": "이미지가 성공적으로 업로드되었습니다.",
  "data": {
    "imageUrl": "http://localhost:9080/uploads/abc-123-def.jpg",
    "fileName": "abc-123-def.jpg",
    "originalFileName": "my-photo.jpg",
    "fileSize": 245678,
    "contentType": "image/jpeg"
  }
}
```

---

### 2. 이미지 삭제

```
DELETE /api/upload/image/{fileName}
Authorization: Bearer {accessToken}
```

---

## curl 테스트 예제

### 1. 헬스체크

```bash
curl -s http://localhost:9080/api/health | jq
```

### 2. 회원가입

```bash
curl -s -X POST http://localhost:9080/api/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "테스트유저"
  }' | jq
```

### 3. 로그인

```bash
curl -s -X POST http://localhost:9080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }' | jq
```

### 4. 토큰을 변수에 저장

```bash
TOKEN=$(curl -s -X POST http://localhost:9080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }' | jq -r '.data.accessToken')

echo "Token: $TOKEN"
```

### 5. 게시글 작성

```bash
curl -s -X POST http://localhost:9080/api/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "content": "오늘 맛있는 저녁 먹었어요! #맛집 #저녁",
    "visibility": "PUBLIC"
  }' | jq
```

### 6. 게시글 목록 조회

```bash
curl -s "http://localhost:9080/api/posts?page=0&size=10" | jq
```

### 7. 탐색 피드 조회

```bash
curl -s "http://localhost:9080/api/feed/explore?page=0&size=10" | jq
```

### 8. 게시글 좋아요

```bash
curl -s -X POST http://localhost:9080/api/posts/1/like \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 9. 팔로우

```bash
curl -s -X POST http://localhost:9080/api/users/2/follow \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 10. 댓글 작성

```bash
curl -s -X POST http://localhost:9080/api/posts/1/comments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "content": "좋은 게시글이네요!"
  }' | jq
```

### 11. 프로필 조회

```bash
curl -s http://localhost:9080/api/user/profile \
  -H "Authorization: Bearer $TOKEN" | jq
```

### 12. 프로필 수정

```bash
curl -s -X PUT http://localhost:9080/api/user/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "새로운닉네임",
    "phoneNumber": "010-1234-5678"
  }' | jq
```

### 13. 이미지 업로드

```bash
curl -s -X POST http://localhost:9080/api/upload/image \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/image.jpg" | jq
```

### 14. 인기 해시태그 조회

```bash
curl -s "http://localhost:9080/api/hashtags/trending/top?limit=10" | jq
```

### 15. 홈 피드 조회

```bash
curl -s "http://localhost:9080/api/feed?page=0&size=10" \
  -H "Authorization: Bearer $TOKEN" | jq
```

---

## 에러 코드 참조

| HTTP 상태 | 의미 |
|-----------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 (유효성 검사 실패) |
| 401 | 인증 필요 / 토큰 만료 |
| 403 | 권한 없음 |
| 404 | 리소스를 찾을 수 없음 |
| 409 | 충돌 (중복 데이터) |
| 500 | 서버 오류 |

---

## 프론트엔드 연동 팁

### 1. Axios 인스턴스 설정

```typescript
import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',  // Vite 프록시 사용 시
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// 요청 인터셉터 - 토큰 자동 추가
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터 - 401 에러 처리
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 토큰 갱신 또는 로그아웃 처리
    }
    return Promise.reject(error);
  }
);
```

### 2. Vite 프록시 설정

```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:9080',
        changeOrigin: true,
      }
    }
  }
});
```

### 3. 카카오 로그인 처리

```typescript
// 카카오 로그인 시작
const handleKakaoLogin = () => {
  const callbackUrl = encodeURIComponent(`${window.location.origin}/auth/kakao/callback`);
  window.location.href = `http://localhost:9080/api/auth/kakao/login?redirectUrl=${callbackUrl}`;
};

// 콜백 페이지에서 토큰 파싱
const hash = window.location.hash.substring(1);
const params = new URLSearchParams(hash);
const accessToken = params.get('accessToken');
const userJson = decodeURIComponent(params.get('user') || '');
const user = JSON.parse(userJson);
```

---

## 타입 정의 (TypeScript)

```typescript
// 공통 응답
interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

// 사용자
interface User {
  id: number;
  email: string;
  name: string;
  role: string;
  profileImage: string | null;
}

// 게시글
interface Post {
  id: number;
  content: string;
  visibility: 'PUBLIC' | 'PRIVATE' | 'FOLLOWERS';
  likeCount: number;
  commentCount: number;
  viewCount: number;
  author: {
    id: number;
    name: string;
    profileImage: string | null;
  };
  images: {
    id: number;
    imageUrl: string;
    thumbnailUrl: string;
    sortOrder: number;
  }[];
  isLiked: boolean;
  isBookmarked: boolean;
  createdAt: string;
  updatedAt: string;
}

// 페이징 응답
interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
```

---

*이 문서는 2026년 2월 7일에 생성되었습니다.*
