package com.daf360.portal.security;

import java.util.List;
import java.util.Set;

/**
 * Les pays que l'appelant a le droit de voir, pour la durée d'une requête.
 *
 * <p>Jumeau volontaire de {@code com.daf360.rh.security.PaysScopeContext} : les deux
 * services ne partagent aucun code, et l'annuaire (ici) comme la liste des profils (rh)
 * doivent appliquer la même règle. Toute modification de l'un doit être portée sur l'autre.
 *
 * <p>Le portail est celui qui RÉSOUT le mode du rôle (OWN / LIST / ALL — V74) et l'émet
 * dans les revendications `paysScopeAll` + `paysIds` du jeton. Il se trouvait ne pas les
 * relire pour ses propres écrans : l'annuaire n'avait aucun filtre pays, et affichait le
 * personnel de toutes les entités à tout le monde.
 *
 * <p>Non résolu ⇒ permissif, comme dans rh-service : un jeton sans `paysId` viderait sinon
 * l'annuaire, ce qui est une panne plus visible qu'une fuite sur une donnée que toute
 * l'entreprise consulte de toute façon.
 */
public final class PaysScopeContext {

    /** Les permissions qui ouvrent la portée à tous les pays — voir rh-service TenantService. */
    private static final Set<String> GLOBAL_PERMISSIONS = Set.of("ADMIN_ROLES", "RH_SUPER_ADMIN");

    public record Scope(boolean all, Set<Long> paysIds) {

        public boolean unfiltered() {
            return all || paysIds == null || paysIds.isEmpty();
        }

        /** Jamais vide : SQL Server refuse un `IN ()`, et -1 ne correspond à aucun pays. */
        public List<Long> idsOrPlaceholder() {
            return unfiltered() ? List.of(-1L) : List.copyOf(paysIds);
        }
    }

    private PaysScopeContext() {}

    private static final ThreadLocal<Scope> HOLDER = new ThreadLocal<>();

    public static void set(Scope scope) { HOLDER.set(scope); }
    public static void clear()          { HOLDER.remove(); }

    /** La portée de la requête en cours, ou une portée ouverte si rien n'a été posé. */
    public static Scope current() {
        Scope scope = HOLDER.get();
        return scope != null ? scope : new Scope(true, Set.of());
    }

    public static boolean isGlobalPermission(String authority) {
        return GLOBAL_PERMISSIONS.contains(authority);
    }
}
