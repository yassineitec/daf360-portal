package com.daf360.portal.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EventCreateDto {
    private String title;
    private LocalDate eventDate;
    private String eventType; // "COMPANY_EVENT", "ANNOUNCEMENT", "OTHER"
    private String description;
    private Long paysId;
}
