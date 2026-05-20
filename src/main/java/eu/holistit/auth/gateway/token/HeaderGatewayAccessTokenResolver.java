package eu.holistit.auth.gateway.token;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class HeaderGatewayAccessTokenResolver implements GatewayAccessTokenResolver {

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String authorization = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return Mono.empty();
        }

        String token = authorization.substring(BEARER_PREFIX.length());

        if (token.isBlank()) {
            return Mono.empty();
        }

        return Mono.just(token);
    }
}