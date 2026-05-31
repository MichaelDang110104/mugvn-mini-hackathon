package com.hackathon.backend.events;

import com.hackathon.backend.dto.EventRequest;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class UserEventReceivedEvent extends ApplicationEvent {

    private final List<EventRequest> requests;

    public UserEventReceivedEvent(Object source, EventRequest request) {
        this(source, request == null ? List.of() : List.of(request));
    }

    public UserEventReceivedEvent(Object source, List<EventRequest> requests) {
        super(source);
        this.requests = requests;
    }

    public List<EventRequest> getRequests() {
        return requests;
    }

    public EventRequest getRequest() {
        return (requests == null || requests.isEmpty()) ? null : requests.getFirst();
    }
}
