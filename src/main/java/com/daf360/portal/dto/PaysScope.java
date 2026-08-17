package com.daf360.portal.dto;

import java.util.List;

/**
 * The set of countries a user is allowed to see, resolved from their role.
 *
 * Two shapes only:
 *   - all = true            → unrestricted, paysIds is empty and must be ignored
 *   - all = false           → exactly paysIds (never empty; the resolver falls back to the
 *                             user's own pays_id rather than emitting an empty restriction)
 *
 * An empty restricted set would mean "see nothing", which no caller wants and which the
 * `IN ()` predicates downstream cannot express — hence the fallback in
 * UserSyncService.extractPaysScope.
 */
public record PaysScope(boolean all, List<Long> paysIds) {

    public static PaysScope unrestricted() {
        return new PaysScope(true, List.of());
    }

    public static PaysScope of(List<Long> paysIds) {
        return new PaysScope(false, List.copyOf(paysIds));
    }
}
