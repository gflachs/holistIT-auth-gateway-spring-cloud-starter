package eu.holistit.auth.gateway.client;

import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class IdentityManagementClient {

    private final WebClient webClient;
    private final HolistitAuthGatewayProperties properties;

    public IdentityManagementClient(
            WebClient.Builder webClientBuilder,
            HolistitAuthGatewayProperties properties) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.identityManagement().baseUrl())
                .build();
    }

    public Mono<ResolveAuthContextResponse> resolve(
            String bearerToken,
            Long organizationId,
            Long tenantId) {
        return webClient.get()
                .uri(properties.identityManagement().resolvePath())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .header(properties.headers().organizationId(), String.valueOf(organizationId))
                .header(properties.headers().tenantId(), String.valueOf(tenantId))
                .retrieve()
                .bodyToMono(ResolveAuthContextResponse.class)
                .timeout(properties.identityManagement().timeout());
    }
}