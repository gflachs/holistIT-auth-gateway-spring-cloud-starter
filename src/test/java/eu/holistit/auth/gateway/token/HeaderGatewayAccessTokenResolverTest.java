package eu.holistit.auth.gateway.token;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderGatewayAccessTokenResolverTest {

    private final HeaderGatewayAccessTokenResolver resolver = new HeaderGatewayAccessTokenResolver();

    @Test
    void shouldResolveBearerTokenFromAuthorizationHeader() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("Authorization", "Bearer abc.def.ghi"));

        StepVerifier.create(resolver.resolve(exchange))
                .assertNext(token -> assertThat(token).isEqualTo("abc.def.ghi"))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenAuthorizationHeaderIsMissing() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test"));

        StepVerifier.create(resolver.resolve(exchange))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenAuthorizationHeaderIsNotBearer() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("Authorization", "Basic abc"));

        StepVerifier.create(resolver.resolve(exchange))
                .verifyComplete();
    }
}