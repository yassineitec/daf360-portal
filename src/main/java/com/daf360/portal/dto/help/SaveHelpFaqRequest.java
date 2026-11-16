package com.daf360.portal.dto.help;

/**
 * Create / update body of an admin help question. French is required; English and Arabic
 * are optional — a blank value is stored as null and the screens fall back to French.
 * Checked by `PortalHelpFaqService` (the portal has no bean-validation starter).
 */
public record SaveHelpFaqRequest(
        String pageKey,
        String questionFr,
        String answerFr,
        String questionEn,
        String answerEn,
        String questionAr,
        String answerAr,
        Integer orderIndex,
        Boolean isActive
) {}
