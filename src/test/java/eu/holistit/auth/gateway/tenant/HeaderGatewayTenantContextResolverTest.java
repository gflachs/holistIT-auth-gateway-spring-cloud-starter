package eu.holistit.auth.gateway.tenant;

import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class HeaderGatewayTenantContextResolverTest {

    private final HolistitAuthGatewayProperties properties = new HolistitAuthGatewayProperties(
            true,
            true,
            true,
            null,
            null,
            null,
            null,
            null);

    private final HeaderGatewayTenantContextResolver resolver = new HeaderGatewayTenantContextResolver(properties);

    @Test
    void shouldResolveTenantContextFromHeaders() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("X-Org-Id", "1")
                        .header("X-Tenant-Id", "10"));

        StepVerifier.create(resolver.resolve(exchange))
                .assertNext(context -> {
                    assertThat(context.organizationId()).isEqualTo(1L);
                    assertThat(context.tenantId()).isEqualTo(10L);
                })
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenHeadersAreMissing() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test"));

        StepVerifier.create(resolver.resolve(exchange))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenHeadersAreInvalid() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("X-Org-Id", "abc")
                        .header("X-Tenant-Id", "10"));

        StepVerifier.create(resolver.resolve(exchange))
                .verifyComplete();
    }
}