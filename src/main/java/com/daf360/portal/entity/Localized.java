package com.daf360.portal.entity;

/**
 * A reference-data row that carries a French and an English label — departments,
 * disciplines, grades, NOG levels.
 *
 * It exists so callers can resolve a label through ONE null-safe helper instead of
 * repeating `x != null && x.getY() != null ? … : null` per field. Every implementor
 * already has both getters from Lombok's {@code @Getter}, so implementing this adds
 * no code to them.
 */
public interface Localized {

    String getLabelFr();

    String getLabelEn();
}
