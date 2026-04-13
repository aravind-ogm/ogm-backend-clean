package com.ogm.market.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Enables @Async and configures dedicated thread pools.
 * Keeps email/SMS/embedding calls off the main request thread.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /** Used by email, SMS, WhatsApp notification services. */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("notification-");
        executor.initialize();
        return executor;
    }

    /**
     * Used by PropertyServiceImpl.generateAndStoreEmbeddingAsync().
     * Keeps Gemini embedding API calls off the property save thread
     * so create/update responses return instantly.
     */
    @Bean(name = "embeddingExecutor")
    public Executor embeddingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("embedding-");
        executor.initialize();
        return executor;
    }
}