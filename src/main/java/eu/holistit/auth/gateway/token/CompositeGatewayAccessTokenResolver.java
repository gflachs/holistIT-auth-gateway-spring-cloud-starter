package eu.holistit.auth.gateway.token;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public class CompositeGatewayAccessTokenResolver implements GatewayAccessTokenResolver {

    private final List<GatewayAccessTokenResolver> delegates;

    public CompositeGatewayAccessTokenResolver(List<GatewayAccessTokenResolver> delegates) {
        this.delegates = List.copyOf(delegates);
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return Flux.fromIterable(delegates)
                .concatMap(delegate -> delegate.resolve(exchange))
                .next();
    }
}