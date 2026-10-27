# 실험 기록

키워드 1차 분류 성능과 LLM 2차 분류 성능을 비교 기록한다.

| 날짜 | 데이터 건수 | 키워드 분류율 | LLM 분류율 | 전체 정확도 | 평균 응답시간(ms) | 비고 |
|------|------------|--------------|-----------|------------|------------------|------|
| 2026-10-26 | 250 | 59.6% (149/250) | - (미구현) | - (측정 불가) | - | 실제 WonnaBE DB `card_transactions` 250건. 키워드 사전 1차 버전(카테고리별 10~20개). 나머지 101건(40.4%)은 UNCLASSIFIED로 남아 Claude API 2차 분류 대상이 됨 |
| 2026-10-27 | 250 | 59.6% (149/250) | 9.2% (23/250) | 68.8% (172/250, 키워드+LLM 합산) | - | Gemini API(`gemini-3.5-flash-lite`) 2차 분류 첫 연동. 101건(UNCLASSIFIED) 중 Redis 캐싱 + `@Async` 스레드풀(5개) 병렬 호출로 재시도, 최종 23건만 LLM 분류 성공하고 78건은 UNCLASSIFIED로 남음(아래 "무료 티어 레이트리밋" 참고) |

## 오늘(10/27) 고친 버그 2개

1. **Redis 포트 충돌로 인한 `StreamCorruptedException`**: `/reclassify` 호출 시 매번 500 에러, 로그에 `StreamCorruptedException: invalid stream header: 7B224063`. Docker 컨테이너의 Redis를 몇 번이나 `FLUSHALL`로 비워도(`DBSIZE` 0, `KEYS *` 결과 없음 확인) 에러가 재발 — 즉 **Redis 데이터 문제가 아니었음**. Redis 통신 로그를 TRACE로 켜고 보니 `ERR unknown command 'HELLO'` 응답이 찍혔는데, Redis 7은 `HELLO`를 당연히 지원하므로 이상함을 느끼고 `Get-NetTCPConnection -LocalPort 6379`로 확인한 결과, PC에 예전부터 떠 있던 **완전히 별개의 네이티브 `redis-server.exe`(버전 3.0.504)**가 6379 포트를 먼저 점유하고 있었음. Spring Boot 앱(Docker 밖에서 네이티브로 실행 중)이 `localhost:6379`로 접속하면 Docker 컨테이너가 아니라 이 네이티브 Redis로 연결됐고, 거기엔 몇 주 전 JSON 직렬화 방식으로 테스트했던 캐시 데이터 71개가 그대로 남아있어 역직렬화가 깨졌던 것. MySQL을 3307 포트로 옮겼던 것과 같은 방식으로, Docker Redis 컨테이너를 `-p 6380:6379`로 재생성하고 `application.yml`의 `redis.port`를 6380으로 변경해 해결.
2. **`@Async` 자가 호출(self-invocation) 버그**: `ReclassificationService` 안에서 `this::classifyAsync`로 같은 클래스의 `@Async` 메서드를 호출하고 있었는데, 스프링 AOP는 프록시 객체를 거친 호출에만 어노테이션 로직을 적용하므로 `this` 직접 호출은 프록시를 우회해 `@Async`가 조용히 무시됨 — 101건이 전부 HTTP 요청 스레드 하나에서 순차 처리되고 있었음. `classifyAsync`를 별도 빈(`AsyncClassificationRunner`)으로 분리해서 반드시 프록시를 거쳐 호출되도록 수정. 수정 후 로그로 `llm-classify-1`~`llm-classify-5` 5개 스레드에 실제로 작업이 분산되는 것을 확인(22/18/21/23/17건).

## 비고
- **전체 정확도를 아직 못 재는 이유**: DB에 원래 있던 `merchant_category` 값(기존 WonnaBE)이 대부분 `other`로 잘못 채워진 더미 데이터라 정답지(ground truth)로 쓸 수 없음. Claude API 분류까지 붙인 뒤 일부 샘플을 수동 검수해서 정확도를 추정할 예정.
- **키워드 분류율 59.6%는 베이스라인**: 스타벅스·CGV처럼 흔한 체인점은 잘 잡히지만, 제주 지역 소규모 식당/수산물 가게 같은 긴 꼬리(long-tail) 상호명은 키워드로 커버 불가 — 바로 이 지점이 LLM 2차 분류가 필요한 이유를 실데이터로 보여줌.
- **왜 101건 중 23건만 LLM 분류됐나**: Gemini 무료 티어는 분당 15회 호출 제한이 있는데, `/reclassify`가 5개 스레드로 거의 동시에 101건을 쏘다 보니 대부분이 `429 RESOURCE_EXHAUSTED`로 실패함. `/reclassify`는 UNCLASSIFIED인 것만 다시 시도하는 멱등(idempotent) 엔드포인트라 재시도 로직 없이도 시간 간격을 두고 여러 번 호출하면 자연히 나머지가 채워지는 구조로 설계함.
