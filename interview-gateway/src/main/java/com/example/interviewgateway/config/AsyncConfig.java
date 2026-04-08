package com.example.interviewgateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mlProcessingExecutor")
    public Executor mlProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);   // Minimum threads waiting
        executor.setMaxPoolSize(15);   // Max threads under heavy load
        executor.setQueueCapacity(50); // Queue up to 50 answers if ML is slow
        executor.setThreadNamePrefix("ML-Processor-");
        executor.initialize();
        return executor;
    }
}