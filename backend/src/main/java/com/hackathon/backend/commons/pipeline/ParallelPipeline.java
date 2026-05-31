package com.hackathon.backend.commons.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ParallelPipeline<C extends TaskContext> extends Task<C> {
    private final List<Task<C>> tasks = new ArrayList<>();

    public ParallelPipeline<C> add(Task<C> task) {
        tasks.add(task);
        return this;
    }

    @Override
    public CompletableFuture<C> execute(C ctx) {
        if (tasks.isEmpty()) return CompletableFuture.completedFuture(ctx);

        List<CompletableFuture<C>> futures = tasks.stream()
                .map(t -> {
                    if (!ctx.isSuccess() || t.shouldSkip(ctx)) {
                        return CompletableFuture.completedFuture(ctx);
                    }

                    return t.execute(ctx)
                        .orTimeout(ctx.timeoutMs(), TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> {
                            ctx.addError(t.name(), ex.getMessage());
                            return ctx;
                        });
                })
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> ctx);
    }
}
