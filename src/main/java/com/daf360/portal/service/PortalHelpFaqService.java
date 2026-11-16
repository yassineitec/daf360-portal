package com.daf360.portal.service;

import com.daf360.portal.dto.help.HelpFaqDto;
import com.daf360.portal.dto.help.SaveHelpFaqRequest;
import com.daf360.portal.entity.PortalHelpFaq;
import com.daf360.portal.repository.PortalHelpFaqRepository;
import com.daf360.portal.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * « Aide & guides » › Gérer les questions, in the shell. The admin list and every write need
 * HR_ADMIN_ROLES — the permission that manages the RH assistant too, so the same
 * administrators handle both. Checked against the token's authorities AND, when the token
 * carries none, against the user's role in the database (the way /api/me resolves them):
 * the shell does not send its permission-bearing Bearer to the portal.
 * The published questions are readable by every signed-in user: the assistant and the help
 * page are open to everyone.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PortalHelpFaqService {

    private static final String ADMIN = "HR_ADMIN_ROLES";

    private final PortalHelpFaqRepository repo;
    private final UserRepository users;
    private final UserSyncService userSync;

    /** Every question, published or not — the admin list. */
    @Transactional(readOnly = true)
    public List<HelpFaqDto> listAll(Authentication auth) {
        requireAdmin(auth);
        return repo.findAllByOrderByPageKeyAscOrderIndexAscIdAsc().stream().map(this::toDto).toList();
    }

    /** Published questions only — the assistant and the help page. */
    @Transactional(readOnly = true)
    public List<HelpFaqDto> listActive() {
        return repo.findByIsActiveTrueOrderByPageKeyAscOrderIndexAscIdAsc().stream().map(this::toDto).toList();
    }

    public HelpFaqDto create(SaveHelpFaqRequest req, Authentication auth) {
        Long actorId = requireAdmin(auth);
        PortalHelpFaq faq = PortalHelpFaq.builder()
                .createdAt(OffsetDateTime.now())
                .createdBy(actorId)
                .build();
        apply(faq, req);
        return toDto(repo.save(faq));
    }

    public HelpFaqDto update(Long id, SaveHelpFaqRequest req, Authentication auth) {
        Long actorId = requireAdmin(auth);
        PortalHelpFaq faq = find(id);
        apply(faq, req);
        faq.setUpdatedAt(OffsetDateTime.now());
        faq.setUpdatedBy(actorId);
        return toDto(repo.save(faq));
    }

    public HelpFaqDto setActive(Long id, boolean active, Authentication auth) {
        Long actorId = requireAdmin(auth);
        PortalHelpFaq faq = find(id);
        faq.setIsActive(active);
        faq.setUpdatedAt(OffsetDateTime.now());
        faq.setUpdatedBy(actorId);
        return toDto(repo.save(faq));
    }

    public void delete(Long id, Authentication auth) {
        requireAdmin(auth);
        repo.delete(find(id));
    }

    /** The caller's user id, once HR_ADMIN_ROLES is confirmed; 403 otherwise. */
    private Long requireAdmin(Authentication auth) {
        if (auth == null || auth.getName() == null) throw new AccessDeniedException("Not signed in");
        Long userId;
        try {
            userId = Long.valueOf(auth.getName());
        } catch (NumberFormatException e) {
            throw new AccessDeniedException("Unknown user");
        }
        boolean inToken = auth.getAuthorities().stream().anyMatch(a -> ADMIN.equals(a.getAuthority()));
        boolean allowed = inToken || users.findById(userId)
                .map(u -> userSync.extractPermissions(u).contains(ADMIN))
                .orElse(false);
        if (!allowed) throw new AccessDeniedException("HR_ADMIN_ROLES required");
        return userId;
    }

    private PortalHelpFaq find(Long id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("PortalHelpFaq " + id));
    }

    /** Same rules as the RH form: page + French required, lengths capped like the columns. */
    private void apply(PortalHelpFaq faq, SaveHelpFaqRequest req) {
        faq.setPageKey(required(req.pageKey(), 80, "pageKey"));
        faq.setQuestionFr(required(req.questionFr(), 300, "questionFr"));
        faq.setAnswerFr(required(req.answerFr(), 2000, "answerFr"));
        faq.setQuestionEn(optional(req.questionEn(), 300, "questionEn"));
        faq.setAnswerEn(optional(req.answerEn(), 2000, "answerEn"));
        faq.setQuestionAr(optional(req.questionAr(), 300, "questionAr"));
        faq.setAnswerAr(optional(req.answerAr(), 2000, "answerAr"));
        if (req.orderIndex() != null) faq.setOrderIndex(req.orderIndex());
        if (req.isActive() != null) faq.setIsActive(req.isActive());
    }

    private static String required(String s, int max, String field) {
        String v = optional(s, max, field);
        if (v == null) throw new IllegalArgumentException(field + " is required");
        return v;
    }

    private static String optional(String s, int max, String field) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.length() > max) throw new IllegalArgumentException(field + " is longer than " + max);
        return v;
    }

    private HelpFaqDto toDto(PortalHelpFaq f) {
        return new HelpFaqDto(f.getId(), f.getPageKey(),
                f.getQuestionFr(), f.getAnswerFr(),
                f.getQuestionEn(), f.getAnswerEn(),
                f.getQuestionAr(), f.getAnswerAr(),
                f.getOrderIndex(), f.getIsActive(),
                f.getCreatedAt(), f.getUpdatedAt());
    }
}
