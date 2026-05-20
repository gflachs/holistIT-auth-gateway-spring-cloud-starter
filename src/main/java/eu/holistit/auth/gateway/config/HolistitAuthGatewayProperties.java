package eu.holistit.auth.gateway.config;

import eu.holistit.auth.core.cache.AuthContextCacheKeys;
import eu.holistit.auth.core.http.AuthHeaders;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "holistit.auth.gateway")
public record HolistitAuthGatewayProperties(
        Boolean enabled,
        Boolean globalAuthContextFilterEnabled,
        Boolean globalHeaderStrippingEnabled,
        Headers headers,
        Redis redis,
        IdentityManagement identityManagement,
        List<String> excludedPaths,
        List<String> strippedHeaders) {

    public HolistitAuthGatewayProperties {
        if (enabled == null) {
            enabled = true;
        }

        if (globalAuthContextFilterEnabled == null) {
            globalAuthContextFilterEnabled = true;
        }

        if (globalHeaderStrippingEnabled == null) {
            globalHeaderStrippingEnabled = true;
        }

        if (headers == null) {
            headers = new Headers(
                    AuthHeaders.X_AUTH_CONTEXT,
                    AuthHeaders.X_ORG_ID,
                    AuthHeaders.X_TENANT_ID);
        }

        if (redis == null) {
            redis = new Redis(AuthContextCacheKeys.DEFAULT_TOKEN_PREFIX);
        }

        if (identityManagement == null) {
            identityManagement = new IdentityManagement(
                    "http://localhost:8090",
                    "/internal/auth-context/resolve",
                    Duration.ofMillis(750));
        }

        if (excludedPaths == null) {
            excludedPaths = List.of(
                    "/actuator/**",
                    "/login/**",
                    "/oauth2/**",
                    "/logout/**",
                    "/public/**",
                    "/assets/**",
                    "/favicon.ico");
        }

        if (strippedHeaders == null) {
            strippedHeaders = List.of(
                    headers.authContext(),
                    headers.organizationId(),
                    headers.tenantId(),
                    "X-Tenant",
                    "Tenant-Id",
                    "X-User-Id",
                    "X-User",
                    "X-User-Subject",
                    "X-Application-User-Id",
                    "X-Roles",
                    "X-Authorities",
                    "X-Scopes",
                    "X-Actor-Type");
        }
    }

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public boolean isGlobalAuthContextFilterEnabled() {
        return Boolean.TRUE.equals(globalAuthContextFilterEnabled);
    }

    public boolean isGlobalHeaderStrippingEnabled() {
        return Boolean.TRUE.equals(globalHeaderStrippingEnabled);
    }

    public record Headers(
            String authContext,
            String organizationId,
            String tenantId) {
    }

    public record Redis(
            String tokenPrefix) {
    }

    public record IdentityManagement(
            String baseUrl,
            String resolvePath,
            Duration timeout) {
    }
}