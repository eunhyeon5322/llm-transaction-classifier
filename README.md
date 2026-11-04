# LLM 거래내역 분류기

카드사 CSV 거래내역을 업로드하면 **키워드 규칙으로 1차 분류하고, 실패한 건만 LLM(Gemini) API로 2차 분류**하는 하이브리드 거래 분류 백엔드입니다.

KB IT's your Life 6기 캡스톤 프로젝트 "WonnaBE"(별도 레포)에서, "카드 거래 1건을 식비/교통/쇼핑 등으로 분류하는 로직"이 빠져있던 걸 발견하고 그 기능 하나만 떼어내 검증해본 사이드 프로젝트입니다.

## 왜 하이브리드인가

```
CSV 업로드
   │
   ▼
키워드 매칭 (빠르고 무료, 흔한 가맹점명만 커버)
   │
   ├─ 매칭 성공 → 바로 저장
   │
   └─ 매칭 실패 ──▶ Gemini API 2차 분류 (느리고 비용 있음, 긴 꼬리 가맹점명 커버)
                      │
                      ├─ Redis 캐시 (같은 가맹점명 재호출 방지)
                      └─ 비동기 스레드풀로 동시 처리
```

키워드만으로는 "스타벅스" 같은 흔한 체인점만 잡히고, "엔에이치엔케이씨피㈜"처럼 업종을 알아야 판단 가능한 이름이나 처음 보는 지역 소상공인 이름은 구조적으로 못 잡습니다. 그 나머지를 LLM이 보완하는 구조입니다.

실제 WonnaBE DB에서 뽑아온 250건으로 테스트한 결과: **키워드 59.6% + LLM 40.4% = 250/250(100%) 분류 완료**. 자세한 수치와 분류 결과 비교는 [docs/experiments.md](docs/experiments.md) 참고.

## 스택

| 영역 | 기술 |
|---|---|
| 언어/프레임워크 | Java 17, Spring Boot 3.3 |
| DB | MySQL 8.0 (JPA) |
| 캐시 | Redis 7 (`@Cacheable`) |
| 비동기 처리 | Spring `@Async` + 전용 스레드풀 |
| LLM | Google Gemini API (`gemini-3.5-flash-lite`, 무료 티어) |
| CSV 파싱 | Apache Commons CSV |
| API 문서 | springdoc-openapi (Swagger UI) |
| 인프라 | Docker Compose (MySQL + Redis) |

## 실행 방법

### 1. MySQL, Redis 띄우기

```bash
docker compose up -d
```

### 2. Gemini API 키 설정

`src/main/resources/application-local.yml`을 직접 만들어서(git에 안 올라감) 아래처럼 키를 넣습니다.

```yaml
gemini:
  api-key: 발급받은_키
```

### 3. 앱 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. Swagger에서 확인

http://localhost:8080/swagger-ui.html

- `POST /api/transactions/upload` — CSV 업로드(키워드 1차 분류까지 자동 적용)
- `GET /api/transactions` — 전체 거래 조회
- `POST /api/transactions/reclassify` — 키워드로 못 잡은 건을 Gemini로 재분류 (멱등 — 몇 번을 호출해도 UNCLASSIFIED인 것만 다시 시도함)

## 테스트

```bash
./gradlew test
```

핵심 분류 로직(키워드 매칭, CSV 파싱, Gemini 응답 파싱, 재분류 집계) 위주로 단위 테스트를 작성했습니다. Gemini API 실제 HTTP 호출은 네트워크가 필요해 단위 테스트 범위에서 제외했고, 이 부분은 `/reclassify`를 실제로 반복 호출해 수동으로 검증했습니다(검증 과정은 `docs/experiments.md` 참고).

## 알게 된 것 / 겪은 버그

개발하면서 겪은 버그 3개(Redis가 포트 충돌로 전혀 다른 프로세스에 연결되고 있었던 문제, `@Async` 자가 호출로 비동기가 무효화됐던 문제, 실패 결과까지 캐싱되어 재시도가 영영 안 먹히던 문제)는 전부 `docs/experiments.md`에 증상 → 원인 → 해결 과정으로 정리해뒀습니다.

## 알려진 한계

- 카테고리가 6개(식비/교통/쇼핑/문화/여행/기타)로 고정돼 있어 호텔·구독서비스 등 일부는 가장 가까운 카테고리로 억지 분류됨
- Gemini 무료 티어의 분당 호출 제한 때문에 대량 재분류 시 `/reclassify`를 여러 번 나눠 호출해야 함(재시도/백오프 로직은 아직 없음)
- "정확도"는 아직 엄밀히 측정되지 않음 — 지금까지 잰 건 "분류율(커버리지)"이고, 정확도는 소수 샘플을 수동으로 눈으로 확인한 수준

## 다음 단계

AWS EC2 배포
