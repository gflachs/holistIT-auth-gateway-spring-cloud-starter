package eu.holistit.auth.gateway.filter;

import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class StripExternalAuthHeadersGlobalFilterTest {

    @Test
    void shouldRemoveConfiguredAuthHeaders() {
        var properties = new HolistitAuthGatewayProperties(
                true,
                true,
                true,
                null,
                null,
                null,
                null,
                null);

        var filter = new StripExternalAuthHeadersGlobalFilter(properties);

        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/test")
                        .header("X-Auth-Context", "fake")
                        .header("X-Org-Id", "999")
                        .header("X-Tenant-Id", "999")
                        .header("X-User-Id", "evil")
                        .header("X-Roles", "admin")
                        .header("Safe-Header", "safe"));

        AtomicReference<org.springframework.web.server.ServerWebExchange> captured = new AtomicReference<>();

        GatewayFilterChain chain = mutatedExchange -> {
            captured.set(mutatedExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        var headers = captured.get().getRequest().getHeaders();

        assertThat(headers.getFirst("X-Auth-Context")).isNull();
        assertThat(headers.getFirst("X-Org-Id")).isNull();
        assertThat(headers.getFirst("X-Tenant-Id")).isNull();
        assertThat(headers.getFirst("X-User-Id")).isNull();
        assertThat(headers.getFirst("X-Roles")).isNull();
        assertThat(headers.getFirst("Safe-Header")).isEqualTo("safe");
    }
}