package com.daf360.portal.service;

import com.daf360.portal.dto.EventResponse;
import com.daf360.portal.repository.EmployeeProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

/**
 * Recurring personal and professional anniversaries for the home calendar.
 *
 *  - BIRTHDAY         — the employee's date of birth, recurring every year
 *  - WORK_ANNIVERSARY — the anniversary of their hire date, i.e. years at the company
 *
 * The two stay separate event types with their own colour and label: "Sophie B." on a
 * calendar tells you nothing about which of the two it is.
 *
 * Only occurrences inside the requested range are produced, and the home calendar asks
 * for a single month — so this never materialises a year of events.
 */
@Service
@RequiredArgsConstructor
public class BirthdayService {

    private final EmployeeProfileRepository employeeProfileRepository;

    /**
     * Cached per (pays, from, to) — see CacheConfig. The home page asks for the same ranges
     * repeatedly (month grid + the 30-day upcoming list, then again on every revisit), and
     * this is the costly half of the events merge.
     */
    @Cacheable(value = "anniversaries", key = "#paysId + '_' + #from + '_' + #to")
    @Transactional(readOnly = true)
    public List<EventResponse> getBirthdaysForRange(Long paysId, LocalDate from, LocalDate to) {
        // One projection query for the whole feed: four columns plus the user's name.
        // This was findAll() over every profile followed by one user lookup PER profile —
        // an N+1 that grew with headcount on every calendar paint, and pulled columns
        // (photo_url, national_id, passport_number…) the calendar never reads.
        List<Object[]> anchors = employeeProfileRepository.findAnniversaryAnchors(paysId);
        List<EventResponse> result = new ArrayList<>();

        for (Object[] row : anchors) {
            Long      profileId   = (Long)      row[0];
            Long      rowPaysId   = (Long)      row[1];
            LocalDate dateOfBirth = (LocalDate) row[2];
            LocalDate hireDate    = (LocalDate) row[3];
            String    maskedName  = maskFullName((String) row[4]);

            addAnnualEvents(dateOfBirth, "BIRTHDAY", maskedName, profileId, rowPaysId, from, to, result);
            addAnnualEvents(hireDate, "WORK_ANNIVERSARY", maskedName, profileId, rowPaysId, from, to, result);
        }

        result.sort((a, b) -> a.getEventDate().compareTo(b.getEventDate()));
        return result;
    }

    private void addAnnualEvents(LocalDate anchor, String eventType, String name,
                                 Long profileId, Long paysId,
                                 LocalDate from, LocalDate to,
                                 List<EventResponse> result) {
        if (anchor == null) return;
        MonthDay monthDay = MonthDay.from(anchor);
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            LocalDate occurrence;
            try {
                occurrence = monthDay.atYear(year);
            } catch (Exception e) {
                continue; // Feb 29 in a non-leap year
            }
            if (occurrence.isBefore(from) || occurrence.isAfter(to)) continue;

            // How many years this occurrence marks. Carried in `description` so the UI can
            // say "5 ans dans l'entreprise" instead of only naming a person.
            int years = occurrence.getYear() - anchor.getYear();

            result.add(EventResponse.builder()
                .id(profileId)
                .title(name)
                .eventDate(occurrence)
                .eventType(eventType)
                .description(years > 0 ? String.valueOf(years) : null)
                .paysId(paysId)
                .editable(false)
                .build());
        }
    }

    /** "Sophie Bernard Dupont" → "Sophie B. D." — first name in full, the rest initialled. */
    private String maskFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Anniversaire";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].charAt(0) + ".";
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(' ').append(parts[i].charAt(0)).append('.');
        }
        return sb.toString();
    }
}
