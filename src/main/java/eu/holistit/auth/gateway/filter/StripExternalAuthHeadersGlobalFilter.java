package eu.holistit.auth.gateway.filter;

import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class StripExternalAuthHeadersGlobalFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -1000;

    private final HolistitAuthGatewayProperties properties;

    public StripExternalAuthHeadersGlobalFilter(HolistitAuthGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest()
                .mutate()
                .headers(headers -> properties.strippedHeaders().forEach(headerName -> {
                    if (headerName != null && !headerName.isBlank()) {
                        headers.remove(headerName);
                    }
                }))
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}