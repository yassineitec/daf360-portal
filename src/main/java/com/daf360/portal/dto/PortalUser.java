package com.daf360.portal.dto;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

/**
 * Wraps the OidcUser returned by Azure AD with our own fields.
 *
 * The azureOid (Azure Object ID) is the key field:
 *  - Unique across all Azure AD apps for the same user
 *  - Immutable – never changes even if email changes
 *  - Used to look up roles from our own database
 */
@Getter
public class PortalUser implements OidcUser {

    private final OidcUser delegate;

    /** Azure AD Object ID – use this as the primary user key */
    private final String azureOid;

    /** Usually user@company.com */
    private final String preferredUsername;

    /** Display name */
    private final String displayName;

    /** Email address */
    private final String email;

    public PortalUser(OidcUser delegate,
                      String azureOid,
                      String preferredUsername,
                      String displayName,
                      String email) {
        this.delegate          = delegate;
        this.azureOid          = azureOid;
        this.preferredUsername = preferredUsername;
        this.displayName       = displayName != null ? displayName : preferredUsername;
        this.email             = email != null ? email : preferredUsername;
    }

    // ── OidcUser delegation ──────────────────────────────────

    @Override
    public Map<String, Object> getClaims() { return delegate.getClaims(); }

    @Override
    public OidcUserInfo getUserInfo() { return delegate.getUserInfo(); }

    @Override
    public OidcIdToken getIdToken() { return delegate.getIdToken(); }

    @Override
    public Map<String, Object> getAttributes() { return delegate.getAttributes(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    /** Returns preferredUsername as the principal name */
    @Override
    public String getName() { return preferredUsername; }
}
