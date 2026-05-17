package com.hackathon.backend.events;

import com.hackathon.backend.dto.EventRequest;
import org.springframework.context.ApplicationEvent;

public class UserEventReceivedEvent extends ApplicationEvent {

    private final EventRequest request;

    public UserEventReceivedEvent(Object source, EventRequest request) {
        super(source);
        this.request = request;
    }

    public EventRequest getRequest() {
        return request;
    }
}
