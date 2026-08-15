package com.vidacotidiana.identity.infrastructure;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the authenticated caller's identity from the validated Keycloak
 * JWT. USER.id is implemented as the Keycloak `sub` claim (a UUID in
 * Keycloak by default) — see the implementation note in
 * docs/development/00-development-baseline.md §6: this is a technical
 * mapping decision, not a change to the approved data model, and avoids a
 * redundant keycloak_id column.
 */
@Component
public class CurrentUser {

    public UUID userId() {
        return UUID.fromString(jwt().getSubject());
    }

    public String email() {
        String email = jwt().getClaimAsString("email");
        if (email == null) {
            throw new IllegalStateException("Keycloak token is missing the 'email' claim; check client scope mapping.");
        }
        return email;
    }

    public String username() {
        return jwt().getClaimAsString("preferred_username");
    }

    private Jwt jwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new IllegalStateException("No authenticated JWT principal in the current security context.");
        }
        return jwtAuth.getToken();
    }
}
