package eu.holistit.auth.gateway.filter;

import eu.holistit.auth.core.token.TokenHashing;
import eu.holistit.auth.gateway.client.IdentityManagementClient;
import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import eu.holistit.auth.gateway.cache.AuthContextRedisCache;
import eu.holistit.auth.gateway.tenant.GatewayTenantContext;
import eu.holistit.auth.gateway.tenant.GatewayTenantContextResolver;
import eu.holistit.auth.gateway.token.GatewayAccessTokenResolver;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
public class AuthContextGlobalFilter implements GlobalFilter, Ordered {

    public static final int ORDER = -900;

    private final HolistitAuthGatewayProperties properties;
    private final GatewayAccessTokenResolver accessTokenResolver;
    private final GatewayTenantContextResolver tenantContextResolver;
    private final AuthContextRedisCache redisCache;
    private final IdentityManagementClient identityManagementClient;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthContextGlobalFilter(
            HolistitAuthGatewayProperties properties,
            GatewayAccessTokenResolver accessTokenResolver,
            GatewayTenantContextResolver tenantContextResolver,
            AuthContextRedisCache redisCache,
            IdentityManagementClient identityManagementClient) {
        this.properties = properties;
        this.accessTokenResolver = accessTokenResolver;
        this.tenantContextResolver = tenantContextResolver;
        this.redisCache = redisCache;
        this.identityManagementClient = identityManagementClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("AuthContextGlobalFilter invoked path={}", exchange.getRequest().getURI().getPath());

        if (isExcluded(exchange)) {
            log.info("AuthContextGlobalFilter skipped excluded path={}", exchange.getRequest().getURI().getPath());
            return chain.filter(exchange);
        }

        return accessTokenResolver.resolve(exchange)
                .doOnNext(token -> log.info("Resolved access token for auth context"))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("No access token available for auth context");
                    return complete(exchange, HttpStatus.UNAUTHORIZED).then(Mono.empty());
                }))
                .flatMap(accessToken -> tenantContextResolver.resolve(exchange)
                        .doOnNext(ctx -> log.info("Resolved tenant context orgId={} tenantId={}",
                                ctx.organizationId(), ctx.tenantId()))
                        .switchIfEmpty(Mono.defer(() -> {
                            log.warn("No tenant context available for auth context");
                            return complete(exchange, HttpStatus.BAD_REQUEST).then(Mono.empty());
                        }))
                        .flatMap(context -> resolveAuthContextToken(accessToken, context)
                                .doOnNext(token -> log.info("Resolved X-Auth-Context token"))
                                .flatMap(authContextToken -> continueWithHeaders(
                                        exchange,
                                        chain,
                                        context,
                                        authContextToken))));
    }

    private Mono<String> resolveAuthContextToken(
            String accessToken,
            GatewayTenantContext context) {
        String sourceTokenHash = TokenHashing.sha256(accessToken);

        return redisCache.findAuthContextToken(
                sourceTokenHash,
                context.organizationId(),
                context.tenantId())
                .switchIfEmpty(Mono.defer(() -> identityManagementClient.resolve(
                        accessToken,
                        context.organizationId(),
                        context.tenantId())
                        .map(response -> response.authContextToken())));
    }

    private Mono<Void> continueWithHeaders(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            GatewayTenantContext context,
            String authContextToken) {
        var request = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.set(properties.headers().authContext(), authContextToken);
                    headers.set(properties.headers().organizationId(), String.valueOf(context.organizationId()));
                    headers.set(properties.headers().tenantId(), String.valueOf(context.tenantId()));
                })
                .build();

        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isExcluded(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();

        return properties.excludedPaths().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private static Mono<Void> complete(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}