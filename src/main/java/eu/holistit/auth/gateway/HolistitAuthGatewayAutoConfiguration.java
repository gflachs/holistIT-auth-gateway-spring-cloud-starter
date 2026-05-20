package eu.holistit.auth.gateway;

import eu.holistit.auth.gateway.client.IdentityManagementClient;
import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import eu.holistit.auth.gateway.filter.AuthContextGlobalFilter;
import eu.holistit.auth.gateway.filter.StripExternalAuthHeadersGatewayFilterFactory;
import eu.holistit.auth.gateway.filter.StripExternalAuthHeadersGlobalFilter;
import eu.holistit.auth.gateway.cache.AuthContextRedisCache;
import eu.holistit.auth.gateway.tenant.GatewayTenantContextResolver;
import eu.holistit.auth.gateway.tenant.HeaderGatewayTenantContextResolver;
import eu.holistit.auth.gateway.token.CompositeGatewayAccessTokenResolver;
import eu.holistit.auth.gateway.token.GatewayAccessTokenResolver;
import eu.holistit.auth.gateway.token.HeaderGatewayAccessTokenResolver;
import eu.holistit.auth.gateway.token.OAuth2AuthorizedClientGatewayAccessTokenResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(HolistitAuthGatewayProperties.class)
@ConditionalOnProperty(prefix = "holistit.auth.gateway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HolistitAuthGatewayAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    GatewayTenantContextResolver gatewayTenantContextResolver(
            HolistitAuthGatewayProperties properties) {
        return new HeaderGatewayTenantContextResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    GatewayAccessTokenResolver gatewayAccessTokenResolver(
            ObjectProvider<ReactiveOAuth2AuthorizedClientManager> authorizedClientManagerProvider) {
        List<GatewayAccessTokenResolver> delegates = new ArrayList<>();

        delegates.add(new HeaderGatewayAccessTokenResolver());

        ReactiveOAuth2AuthorizedClientManager manager = authorizedClientManagerProvider.getIfAvailable();

        if (manager != null) {
            delegates.add(new OAuth2AuthorizedClientGatewayAccessTokenResolver(manager));
        }

        return new CompositeGatewayAccessTokenResolver(delegates);
    }

    @Bean
    @ConditionalOnMissingBean
    AuthContextRedisCache authContextRedisCache(
            ReactiveStringRedisTemplate redisTemplate,
            HolistitAuthGatewayProperties properties) {
        return new AuthContextRedisCache(redisTemplate, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    IdentityManagementClient identityManagementClient(
            WebClient.Builder webClientBuilder,
            HolistitAuthGatewayProperties properties) {
        return new IdentityManagementClient(webClientBuilder, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    StripExternalAuthHeadersGatewayFilterFactory stripExternalAuthHeadersGatewayFilterFactory(
            HolistitAuthGatewayProperties properties) {
        return new StripExternalAuthHeadersGatewayFilterFactory(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "holistit.auth.gateway", name = "global-header-stripping-enabled", havingValue = "true", matchIfMissing = true)
    StripExternalAuthHeadersGlobalFilter stripExternalAuthHeadersGlobalFilter(
            HolistitAuthGatewayProperties properties) {
        return new StripExternalAuthHeadersGlobalFilter(properties);
    }

    @Bean
    @ConditionalOnProperty(prefix = "holistit.auth.gateway", name = "global-auth-context-filter-enabled", havingValue = "true", matchIfMissing = true)
    AuthContextGlobalFilter authContextGlobalFilter(
            HolistitAuthGatewayProperties properties,
            GatewayAccessTokenResolver accessTokenResolver,
            GatewayTenantContextResolver tenantContextResolver,
            AuthContextRedisCache redisCache,
            IdentityManagementClient identityManagementClient) {
        return new AuthContextGlobalFilter(
                properties,
                accessTokenResolver,
                tenantContextResolver,
                redisCache,
                identityManagementClient);
    }

    @Bean
    @ConditionalOnMissingBean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}