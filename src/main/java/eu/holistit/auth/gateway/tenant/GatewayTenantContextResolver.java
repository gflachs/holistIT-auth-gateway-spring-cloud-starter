package eu.holistit.auth.gateway.tenant;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface GatewayTenantContextResolver {

    Mono<GatewayTenantContext> resolve(ServerWebExchange exchange);
}