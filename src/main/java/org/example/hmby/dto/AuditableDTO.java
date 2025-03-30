package org.example.hmby.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class AuditableDTO {
    private Long id;
    private String userId;
    private Instant createdDate;
    private Instant lastUpdateDate;
}
