package com.daf360.portal.service;

import com.daf360.portal.dto.EventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class LeaveEventService {

    private final JdbcTemplate rhJdbc;

    public LeaveEventService(@Qualifier("rhJdbcTemplate") JdbcTemplate rhJdbc) {
        this.rhJdbc = rhJdbc;
    }

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "CONGE",       "Congé",
            "MALADIE",     "Arrêt maladie",
            "MATERNITE",   "Congé maternité",
            "PATERNITE",   "Congé paternité",
            "EXCEPTIONNEL","Congé exceptionnel",
            "DEUIL_AUTRE", "Congé deuil / autre"
    );

    private static final ZoneId LOCAL_ZONE = ZoneId.of("Europe/Paris");

    public List<EventResponse> getLeaveEvents(String email, LocalDate from, LocalDate to) {
        // dateDebut / dateFin are datetimeoffset stored as UTC midnight of the local date
        String sql = """
                SELECT a.id,
                       a.type,
                       CAST(a.dateDebut AT TIME ZONE 'Romance Standard Time' AS DATE) AS dateDebut,
                       CAST(a.dateFin   AT TIME ZONE 'Romance Standard Time' AS DATE) AS dateFin,
                       a.etatDemande,
                       a.reason
                FROM [dbo].[absences] a
                JOIN [dbo].[Users] u ON a.collaborateur_id = u.id
                WHERE u.email = ?
                  AND CAST(a.dateFin   AT TIME ZONE 'Romance Standard Time' AS DATE) >= ?
                  AND CAST(a.dateDebut AT TIME ZONE 'Romance Standard Time' AS DATE) <= ?
                  AND a.etatDemande NOT IN ('REFUSE')
                ORDER BY a.dateDebut
                """;

        List<EventResponse> events = new ArrayList<>();

        try {
            List<Map<String, Object>> rows = rhJdbc.queryForList(sql, email, from, to);

            for (Map<String, Object> row : rows) {
                LocalDate start    = toLocalDate(row.get("dateDebut"));
                LocalDate end      = toLocalDate(row.get("dateFin"));
                String type        = (String) row.get("type");
                String rawStatus   = (String) row.get("etatDemande");
                Long   id          = ((Number) row.get("id")).longValue();
                String reason      = (String) row.get("reason");

                if (start == null || end == null) continue;

                String label  = TYPE_LABELS.getOrDefault(type, type);
                String status = "VALIDE".equals(rawStatus) ? "APPROVED" : "PENDING";

                LocalDate cursor   = start.isBefore(from) ? from : start;
                LocalDate rangeEnd = end.isAfter(to)      ? to   : end;

                while (!cursor.isAfter(rangeEnd)) {
                    events.add(EventResponse.builder()
                            .id(id)
                            .title(label)
                            .eventDate(cursor)
                            .eventType(type)
                            .description(reason)
                            .paysId(null)
                            .editable(false)
                            .status(status)
                            .build());
                    cursor = cursor.plusDays(1);
                }
            }
        } catch (Exception ex) {
            log.warn("Could not load leave events for {}: {}", email, ex.getMessage());
        }

        return events;
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Date d)      return d.toLocalDate();
        if (value instanceof LocalDate ld)          return ld;
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant().atZone(LOCAL_ZONE).toLocalDate();
        if (value instanceof OffsetDateTime odt)    return odt.atZoneSameInstant(LOCAL_ZONE).toLocalDate();
        return null;
    }
}
