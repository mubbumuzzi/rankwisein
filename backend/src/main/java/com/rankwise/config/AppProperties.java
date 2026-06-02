package com.rankwise.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Admin admin = new Admin();
    private final PdfImport pdfImport = new PdfImport();
    private final Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Admin {
        private String username;
        private String email;
        private String password;
    }

    @Getter
    @Setter
    public static class PdfImport {
        private String storageDir;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
    }
}
