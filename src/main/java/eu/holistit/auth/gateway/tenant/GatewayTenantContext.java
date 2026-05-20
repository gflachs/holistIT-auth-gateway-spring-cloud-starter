package eu.holistit.auth.gateway.tenant;

public record GatewayTenantContext(
        Long organizationId,
        Long tenantId) {
    public GatewayTenantContext {
        if (organizationId == null) {
            throw new IllegalArgumentException("organizationId must not be null");
        }
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId must not be null");
        }
    }
}