package com.daf360.portal.controller;

import com.daf360.portal.dto.EventCreateDto;
import com.daf360.portal.dto.EventResponse;
import com.daf360.portal.dto.EventUpdateDto;
import com.daf360.portal.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/portal/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEventsForRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long paysId,
            Authentication authentication) {
        Long userId = authentication != null ? Long.valueOf((String) authentication.getPrincipal()) : null;
        return ResponseEntity.ok(eventService.getEventsForRange(paysId, from, to, userId));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<EventResponse>> getUpcoming(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long paysId,
            Authentication authentication) {
        Long userId = authentication != null ? Long.valueOf((String) authentication.getPrincipal()) : null;
        return ResponseEntity.ok(eventService.getUpcomingEvents(paysId, days, userId));
    }

    @PostMapping
    @PreAuthorize("hasPermission(null, 'MANAGE_EVENTS')")
    public ResponseEntity<EventResponse> createEvent(
            @RequestBody EventCreateDto dto,
            Authentication authentication) {
        Long userId = Long.valueOf((String) authentication.getPrincipal());
        return ResponseEntity.ok(eventService.createEvent(dto, userId));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MANAGE_EVENTS')")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @RequestBody EventUpdateDto dto) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasPermission(null, 'MANAGE_EVENTS')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
