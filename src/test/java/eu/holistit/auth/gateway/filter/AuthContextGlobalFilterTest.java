package eu.holistit.auth.gateway.filter;

import eu.holistit.auth.gateway.client.IdentityManagementClient;
import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import eu.holistit.auth.gateway.cache.AuthContextRedisCache;
import eu.holistit.auth.gateway.tenant.GatewayTenantContext;
import eu.holistit.auth.gateway.tenant.GatewayTenantContextResolver;
import eu.holistit.auth.gateway.token.GatewayAccessTokenResolver;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthContextGlobalFilterTest {

    @Test
    void shouldSetAuthContextFromRedisCacheHit() {
        var properties = defaultProperties();

        GatewayAccessTokenResolver accessTokenResolver = mock(GatewayAccessTokenResolver.class);
        GatewayTenantContextResolver tenantContextResolver = mock(GatewayTenantContextResolver.class);
        AuthContextRedisCache redisCache = mock(AuthContextRedisCache.class);
        IdentityManagementClient identityManagementClient = mock(IdentityManagementClient.class);

        var filter = new AuthContextGlobalFilter(
                properties,
                accessTokenResolver,
                tenantContextResolver,
                redisCache,
                identityManagementClient);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test"));

        when(accessTokenResolver.resolve(exchange)).thenReturn(Mono.just("raw-access-token"));
        when(tenantContextResolver.resolve(exchange)).thenReturn(Mono.just(new GatewayTenantContext(1L, 10L)));
        when(redisCache.findAuthContextToken(anyString(), eq(1L), eq(10L)))
                .thenReturn(Mono.just("cached-auth-context-token"));

        AtomicReference<org.springframework.web.server.ServerWebExchange> captured = new AtomicReference<>();

        GatewayFilterChain chain = mutatedExchange -> {
            captured.set(mutatedExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        var headers = captured.get().getRequest().getHeaders();

        assertThat(headers.getFirst("X-Auth-Context")).isEqualTo("cached-auth-context-token");
        assertThat(headers.getFirst("X-Org-Id")).isEqualTo("1");
        assertThat(headers.getFirst("X-Tenant-Id")).isEqualTo("10");

        verify(identityManagementClient, never()).resolve(anyString(), anyLong(), anyLong());
    }

    @Test
    void shouldResolveAuthContextFromIdentityManagementOnCacheMiss() {
        var properties = defaultProperties();

        GatewayAccessTokenResolver accessTokenResolver = mock(GatewayAccessTokenResolver.class);
        GatewayTenantContextResolver tenantContextResolver = mock(GatewayTenantContextResolver.class);
        AuthContextRedisCache redisCache = mock(AuthContextRedisCache.class);
        IdentityManagementClient identityManagementClient = mock(IdentityManagementClient.class);

        var filter = new AuthContextGlobalFilter(
                properties,
                accessTokenResolver,
                tenantContextResolver,
                redisCache,
                identityManagementClient);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test"));

        when(accessTokenResolver.resolve(exchange)).thenReturn(Mono.just("raw-access-token"));
        when(tenantContextResolver.resolve(exchange)).thenReturn(Mono.just(new GatewayTenantContext(1L, 10L)));
        when(redisCache.findAuthContextToken(anyString(), eq(1L), eq(10L)))
                .thenReturn(Mono.empty());

        when(identityManagementClient.resolve("raw-access-token", 1L, 10L))
                .thenReturn(Mono.just(new eu.holistit.auth.gateway.client.ResolveAuthContextResponse(
                        "resolved-auth-context-token",
                        java.time.Instant.parse("2026-05-18T12:05:00Z"),
                        "auth-context")));

        AtomicReference<org.springframework.web.server.ServerWebExchange> captured = new AtomicReference<>();

        GatewayFilterChain chain = mutatedExchange -> {
            captured.set(mutatedExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        var headers = captured.get().getRequest().getHeaders();

        assertThat(headers.getFirst("X-Auth-Context")).isEqualTo("resolved-auth-context-token");
        assertThat(headers.getFirst("X-Org-Id")).isEqualTo("1");
        assertThat(headers.getFirst("X-Tenant-Id")).isEqualTo("10");

        verify(identityManagementClient).resolve("raw-access-token", 1L, 10L);
    }

    @Test
    void shouldReturnUnauthorizedWhenNoAccessTokenIsAvailable() {
        var properties = defaultProperties();

        GatewayAccessTokenResolver accessTokenResolver = mock(GatewayAccessTokenResolver.class);
        GatewayTenantContextResolver tenantContextResolver = mock(GatewayTenantContextResolver.class);
        AuthContextRedisCache redisCache = mock(AuthContextRedisCache.class);
        IdentityManagementClient identityManagementClient = mock(IdentityManagementClient.class);

        var filter = new AuthContextGlobalFilter(
                properties,
                accessTokenResolver,
                tenantContextResolver,
                redisCache,
                identityManagementClient);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test"));

        when(accessTokenResolver.resolve(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, mutated -> Mono.empty()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldSkipExcludedPath() {
        var properties = defaultProperties();

        GatewayAccessTokenResolver accessTokenResolver = mock(GatewayAccessTokenResolver.class);
        GatewayTenantContextResolver tenantContextResolver = mock(GatewayTenantContextResolver.class);
        AuthContextRedisCache redisCache = mock(AuthContextRedisCache.class);
        IdentityManagementClient identityManagementClient = mock(IdentityManagementClient.class);

        var filter = new AuthContextGlobalFilter(
                properties,
                accessTokenResolver,
                tenantContextResolver,
                redisCache,
                identityManagementClient);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/actuator/health"));

        AtomicReference<Boolean> called = new AtomicReference<>(false);

        GatewayFilterChain chain = mutatedExchange -> {
            called.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        assertThat(called.get()).isTrue();
        verifyNoInteractions(accessTokenResolver);
    }

    private static HolistitAuthGatewayProperties defaultProperties() {
        return new HolistitAuthGatewayProperties(
                true,
                true,
                true,
                null,
                null,
                null,
                null,
                null);
    }
}