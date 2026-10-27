package com.wonnabe.classifier.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	/**
	 * UNCLASSIFIED 거래를 LLM으로 재분류할 때 동시에 처리할 스레드 수를 제한한다.
	 * 무료 티어 API는 분당 호출 제한이 있어서, 무제한 병렬 호출 대신 소규모로 동시성을 둔다.
	 */
	@Bean(name = "llmTaskExecutor")
	public Executor llmTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("llm-classify-");
		executor.initialize();
		return executor;
	}
}
