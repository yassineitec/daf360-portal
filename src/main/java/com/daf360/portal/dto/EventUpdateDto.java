package com.daf360.portal.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EventUpdateDto {
    private String title;
    private LocalDate eventDate;
    private String eventType;
    private String description;
    private Long paysId;
}
