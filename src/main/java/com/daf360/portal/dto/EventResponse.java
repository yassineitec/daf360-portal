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
    private String eventType; // "BIRTHDAY", "COMPANY_EVENT", "ANNOUNCEMENT", "OTHER"
    private String description;
    private Long paysId;
    private boolean editable; // true only for COMPANY_EVENT/ANNOUNCEMENT/OTHER (not birthdays)
}
