package pl.mostalow.nbp_exchange_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "nbp.api")
public class NBPApiProperties {
    private Paths paths;

    @Getter
    @Setter
    public static class Paths {
        private String base;
        private String tableA;
        private String tableC;
    }
}