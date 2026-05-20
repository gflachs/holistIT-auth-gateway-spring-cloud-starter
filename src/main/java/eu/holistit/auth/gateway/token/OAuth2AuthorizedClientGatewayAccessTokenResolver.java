package eu.holistit.auth.gateway.token;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class OAuth2AuthorizedClientGatewayAccessTokenResolver implements GatewayAccessTokenResolver {

    private final ReactiveOAuth2AuthorizedClientManager authorizedClientManager;

    public OAuth2AuthorizedClientGatewayAccessTokenResolver(
            ReactiveOAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .ofType(OAuth2AuthenticationToken.class)
                .flatMap(authentication -> {
                    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                            .withClientRegistrationId(authentication.getAuthorizedClientRegistrationId())
                            .principal(authentication)
                            .attribute(ServerWebExchange.class.getName(), exchange)
                            .build();

                    return authorizedClientManager.authorize(authorizeRequest);
                })
                .map(authorizedClient -> authorizedClient.getAccessToken().getTokenValue());
    }
}