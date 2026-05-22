package com.hackathon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatchEventResponse {
    private int accepted;
    private int failed;
}
