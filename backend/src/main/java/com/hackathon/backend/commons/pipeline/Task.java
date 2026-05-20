package com.hackathon.backend.commons.pipeline;

import java.util.concurrent.CompletableFuture;

public abstract class Task<C extends TaskContext> {

    public abstract CompletableFuture<C> execute(C ctx);

    public String name() {
        return this.getClass().getSimpleName();
    }

    public boolean shouldSkip(C ctx) {
        return false;
    }

    public boolean stopOnFailure() {
        return false;
    }
}
