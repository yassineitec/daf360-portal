package com.daf360.portal.service;

import com.daf360.portal.dto.EventCreateDto;
import com.daf360.portal.dto.EventResponse;
import com.daf360.portal.dto.EventUpdateDto;
import com.daf360.portal.entity.PortalEvent;
import com.daf360.portal.repository.PortalEventRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final PortalEventRepository portalEventRepository;
    private final BirthdayService birthdayService;

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsForRange(Long paysId, LocalDate from, LocalDate to) {
        List<EventResponse> events = new ArrayList<>();
        portalEventRepository.findActiveEventsInRange(from, to, paysId)
            .forEach(e -> events.add(toResponse(e)));
        events.addAll(birthdayService.getBirthdaysForRange(paysId, from, to));
        events.sort(Comparator.comparing(EventResponse::getEventDate));
        return events;
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents(Long paysId, int days) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(days);
        return getEventsForRange(paysId, from, to);
    }

    @Transactional
    public EventResponse createEvent(EventCreateDto dto, Long createdBy) {
        PortalEvent event = new PortalEvent();
        event.setTitle(dto.getTitle());
        event.setEventDate(dto.getEventDate());
        event.setEventType(dto.getEventType());
        event.setDescription(dto.getDescription());
        event.setPaysId(dto.getPaysId());
        event.setCreatedBy(createdBy);
        event.setDeleted(false);
        return toResponse(portalEventRepository.save(event));
    }

    @Transactional
    public EventResponse updateEvent(Long id, EventUpdateDto dto) {
        PortalEvent event = portalEventRepository.findById(id)
            .filter(e -> !e.isDeleted())
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getEventType() != null) event.setEventType(dto.getEventType());
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getPaysId() != null) event.setPaysId(dto.getPaysId());
        return toResponse(portalEventRepository.save(event));
    }

    @Transactional
    public void deleteEvent(Long id) {
        PortalEvent event = portalEventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
        event.setDeleted(true);
        portalEventRepository.save(event);
    }

    private EventResponse toResponse(PortalEvent e) {
        return EventResponse.builder()
            .id(e.getId())
            .title(e.getTitle())
            .eventDate(e.getEventDate())
            .eventType(e.getEventType())
            .description(e.getDescription())
            .paysId(e.getPaysId())
            .editable(true)
            .build();
    }
}
