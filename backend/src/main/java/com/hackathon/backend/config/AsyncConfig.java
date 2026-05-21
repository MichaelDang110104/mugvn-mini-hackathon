package com.hackathon.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("event-proc-");
        executor.initialize();
        return executor;
    }

    @Bean("ioExecutor")
    public Executor ioExecutor() {
        var pool = new SimpleAsyncTaskExecutor("io-thread-");

        pool.setVirtualThreads(true);

        return pool;
    }

    @Bean("cpuExecutor")
    public Executor cpuExecutor() {
        var cores = Runtime.getRuntime().availableProcessors();

        var pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(cores);
        pool.setMaxPoolSize(cores);
        pool.setQueueCapacity(cores * 20);
        pool.setThreadNamePrefix("cpu-");
        pool.initialize();

        return pool;
    }
}
