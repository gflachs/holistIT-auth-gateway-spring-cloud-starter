package eu.holistit.auth.gateway.client;

import java.time.Instant;

public record ResolveAuthContextResponse(
        String authContextToken,
        Instant expiresAt,
        String tokenType) {
}