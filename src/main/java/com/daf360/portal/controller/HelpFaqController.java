package com.daf360.portal.controller;

import com.daf360.portal.dto.help.HelpFaqDto;
import com.daf360.portal.dto.help.SaveHelpFaqRequest;
import com.daf360.portal.service.PortalHelpFaqService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Shell « Aide & guides » › Gérer les questions, and the published questions read by the assistant. */
@RestController
@RequestMapping("/api/portal/help-faq")
@RequiredArgsConstructor
public class HelpFaqController {

    private final PortalHelpFaqService service;

    /** Every question (admin list). */
    @GetMapping
    public List<HelpFaqDto> list(Authentication auth) {
        return service.listAll(auth);
    }

    /** Published questions (assistant, help page) — any signed-in user. */
    @GetMapping("/active")
    public List<HelpFaqDto> listActive() {
        return service.listActive();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HelpFaqDto create(@RequestBody SaveHelpFaqRequest req, Authentication auth) {
        return service.create(req, auth);
    }

    @PutMapping("/{id}")
    public HelpFaqDto update(@PathVariable Long id, @RequestBody SaveHelpFaqRequest req, Authentication auth) {
        return service.update(id, req, auth);
    }

    @PatchMapping("/{id}/activate")
    public HelpFaqDto activate(@PathVariable Long id, Authentication auth) {
        return service.setActive(id, true, auth);
    }

    @PatchMapping("/{id}/deactivate")
    public HelpFaqDto deactivate(@PathVariable Long id, Authentication auth) {
        return service.setActive(id, false, auth);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth);
    }
}
