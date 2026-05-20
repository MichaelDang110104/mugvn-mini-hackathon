package com.hackathon.backend.commons.pipeline;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskContext {
    private boolean success = true;
    private int timeoutMs = 300;
    private final Map<String, String> errors = new ConcurrentHashMap<>();
    private final Map<String, Object> extra  = new ConcurrentHashMap<>();

    public boolean isSuccess()    { return success; }
    public int     timeoutMs()    { return timeoutMs; }
    public Map<String, String> getErrors() { return errors; }
    public Map<String, Object> getExtra()  { return extra; }

    public void setSuccess(boolean success)   { this.success = success; }
    public void setTimeoutMs(int timeoutMs)   { this.timeoutMs = timeoutMs; }

    public void addError(String taskName, String message) {
        errors.put(taskName, message);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) extra.get(key);
    }

    public void set(String key, Object value) {
        extra.put(key, value);
    }
}