package eu.holistit.auth.gateway.token;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public interface GatewayAccessTokenResolver {

    Mono<String> resolve(ServerWebExchange exchange);
}