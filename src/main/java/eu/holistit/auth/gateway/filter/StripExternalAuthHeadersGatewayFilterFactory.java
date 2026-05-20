package eu.holistit.auth.gateway.filter;

import eu.holistit.auth.gateway.config.HolistitAuthGatewayProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;

import java.util.ArrayList;
import java.util.List;

public class StripExternalAuthHeadersGatewayFilterFactory
        extends AbstractGatewayFilterFactory<StripExternalAuthHeadersGatewayFilterFactory.Config> {

    private final HolistitAuthGatewayProperties properties;

    public StripExternalAuthHeadersGatewayFilterFactory(HolistitAuthGatewayProperties properties) {
        super(Config.class);
        this.properties = properties;
    }

    @Override
    public GatewayFilter apply(Config config) {
        List<String> headersToStrip = new ArrayList<>(properties.strippedHeaders());

        if (config.headers != null) {
            headersToStrip.addAll(config.headers);
        }

        return (exchange, chain) -> {
            var request = exchange.getRequest()
                    .mutate()
                    .headers(headers -> headersToStrip.forEach(headers::remove))
                    .build();

            return chain.filter(exchange.mutate().request(request).build());
        };
    }

    public static class Config {
        private List<String> headers = List.of();

        public List<String> getHeaders() {
            return headers;
        }

        public void setHeaders(List<String> headers) {
            this.headers = headers;
        }
    }
}