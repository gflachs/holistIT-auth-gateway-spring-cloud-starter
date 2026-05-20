package eu.holistit.auth.gateway.token;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

class CompositeGatewayAccessTokenResolverTest {

    @Test
    void shouldUseFirstResolverThatReturnsToken() {
        GatewayAccessTokenResolver first = mock(GatewayAccessTokenResolver.class);
        GatewayAccessTokenResolver second = mock(GatewayAccessTokenResolver.class);

        var exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/api/test"));

        when(first.resolve(exchange)).thenReturn(Mono.empty());
        when(second.resolve(exchange)).thenReturn(Mono.just("token-from-second"));

        var resolver = new CompositeGatewayAccessTokenResolver(List.of(first, second));

        StepVerifier.create(resolver.resolve(exchange))
                .expectNext("token-from-second")
                .verifyComplete();

        verify(first).resolve(exchange);
        verify(second).resolve(exchange);
    }

    @Test
    void shouldReturnEmptyWhenNoResolverFindsToken() {
        GatewayAccessTokenResolver first = mock(GatewayAccessTokenResolver.class);
        GatewayAccessTokenResolver second = mock(GatewayAccessTokenResolver.class);

        var exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/api/test"));

        when(first.resolve(exchange)).thenReturn(Mono.empty());
        when(second.resolve(exchange)).thenReturn(Mono.empty());

        var resolver = new CompositeGatewayAccessTokenResolver(List.of(first, second));

        StepVerifier.create(resolver.resolve(exchange))
                .verifyComplete();
    }
}