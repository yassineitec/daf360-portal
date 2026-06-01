package com.daf360.portal.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private LocalDate eventDate;
    private String eventType;
    private String description;
    private Long paysId;
    private boolean editable;
    /** Non-null only for leave events: "APPROVED" or "PENDING" */
    private String status;
}
