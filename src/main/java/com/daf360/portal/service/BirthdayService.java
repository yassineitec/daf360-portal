package com.daf360.portal.service;

import com.daf360.portal.dto.EventResponse;
import com.daf360.portal.entity.EmployeeProfile;
import com.daf360.portal.repository.EmployeeProfileRepository;
import com.daf360.portal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BirthdayService {

    private final EmployeeProfileRepository employeeProfileRepository;
    private final UserRepository userRepository;

    public List<EventResponse> getBirthdaysForRange(Long paysId, LocalDate from, LocalDate to) {
        List<EmployeeProfile> profiles = employeeProfileRepository.findAll();
        List<EventResponse> result = new ArrayList<>();

        for (EmployeeProfile ep : profiles) {
            if (ep.getDateOfBirth() == null) continue;
            if (paysId != null && !paysId.equals(ep.getPaysId())) continue;

            MonthDay birthday = MonthDay.from(ep.getDateOfBirth());
            String maskedName = getMaskedName(ep.getUserId());

            for (int year = from.getYear(); year <= to.getYear(); year++) {
                LocalDate occurrence;
                try {
                    occurrence = birthday.atYear(year);
                } catch (Exception e) {
                    continue; // Feb 29 in non-leap year
                }
                if (!occurrence.isBefore(from) && !occurrence.isAfter(to)) {
                    result.add(EventResponse.builder()
                        .id(ep.getId())
                        .title(maskedName)
                        .eventDate(occurrence)
                        .eventType("BIRTHDAY")
                        .description(null)
                        .paysId(ep.getPaysId())
                        .editable(false)
                        .build());
                }
            }
        }

        result.sort((a, b) -> a.getEventDate().compareTo(b.getEventDate()));
        return result;
    }

    private String getMaskedName(Long userId) {
        if (userId == null) return "Anniversaire";
        return userRepository.findById(userId)
            .map(u -> maskFullName(u.getFullName()))
            .orElse("Anniversaire");
    }

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
