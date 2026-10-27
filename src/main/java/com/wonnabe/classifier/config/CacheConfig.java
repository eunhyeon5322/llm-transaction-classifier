package com.wonnabe.classifier.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * 같은 가맹점명이 반복될 때 LLM을 다시 호출하지 않도록 Redis 캐시를 활성화한다.
 * (스타벅스처럼 자주 등장하는 가맹점명에 대한 중복 API 호출을 줄이는 목적)
 *
 * 캐싱할 타입(ClassificationResult)이 Serializable을 구현하므로 Spring Boot 기본 Redis
 * 직렬화(Java 직렬화)를 그대로 사용한다. JSON 직렬화로 커스터마이징했을 때 원인 불명으로
 * @Cacheable이 메서드 본문을 아예 호출하지 않는 문제가 있어서, 가장 단순한 기본 설정으로 되돌림.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
