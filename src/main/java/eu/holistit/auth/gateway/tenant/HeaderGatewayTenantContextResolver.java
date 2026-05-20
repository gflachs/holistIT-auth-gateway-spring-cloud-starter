package eu.holistit.auth.gateway.tenant;

import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class HeaderGatewayTenantContextResolver implements GatewayTenantContextResolver {

    private final HolistitAuthGatewayProperties properties;

    public HeaderGatewayTenantContextResolver(HolistitAuthGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<GatewayTenantContext> resolve(ServerWebExchange exchange) {
        String organizationId = exchange.getRequest()
                .getHeaders()
                .getFirst(properties.headers().organizationId());

        String tenantId = exchange.getRequest()
                .getHeaders()
                .getFirst(properties.headers().tenantId());

        if (organizationId == null || tenantId == null) {
            return Mono.empty();
        }

        try {
            return Mono.just(new GatewayTenantContext(
                    Long.valueOf(organizationId),
                    Long.valueOf(tenantId)));
        } catch (NumberFormatException ex) {
            return Mono.empty();
        }
    }
}