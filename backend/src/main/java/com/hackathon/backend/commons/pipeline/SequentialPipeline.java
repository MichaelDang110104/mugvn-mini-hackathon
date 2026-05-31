package com.hackathon.backend.commons.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class SequentialPipeline<C extends TaskContext> extends Task<C> {
    private final List<Task<C>> tasks = new ArrayList<>();

    public SequentialPipeline<C> then(Task<C> task) {
        tasks.add(task);
        return this;
    }

    @Override
    public CompletableFuture<C> execute(C ctx) {
        CompletableFuture<C> chain = CompletableFuture.completedFuture(ctx);

        for (Task<C> task : tasks) {
            chain = chain.thenCompose(c -> {
                if (!c.isSuccess() || task.shouldSkip(c)) return CompletableFuture.completedFuture(c);

                return task.execute(c)
                        .orTimeout(c.timeoutMs(), TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> {
                            c.addError(task.name(), ex.getMessage());
                            if (task.stopOnFailure()) {
                                c.setSuccess(false);
                            }
                            return c;
                        });
            });
        }

        return chain;
    }
}