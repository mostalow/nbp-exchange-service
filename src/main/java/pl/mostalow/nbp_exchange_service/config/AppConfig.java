package pl.mostalow.nbp_exchange_service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import static pl.mostalow.nbp_exchange_service.config.WebClientFilters.logRequest;
import static pl.mostalow.nbp_exchange_service.config.WebClientFilters.logResponse;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NBPApiProperties.class)
public class AppConfig {

    private final NBPApiProperties nbpApiProperties;

    @Bean
    public WebClient cryptoWebClient() {
        return WebClient.builder()
                .baseUrl(nbpApiProperties.getPaths().getBase())
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }
}

