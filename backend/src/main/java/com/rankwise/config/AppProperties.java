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
    private final Cursor cursor = new Cursor();
    private final Chat chat = new Chat();

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

    @Getter
    @Setter
    public static class Cursor {
        private String apiKey;
        private String model;
        private boolean enabled;
        private long pollIntervalMs;
        private long maxWaitMs;
        private String agentNamePrefix;
    }

    @Getter
    @Setter
    public static class Chat {
        private int rateLimitPerHour;
        private int maxMessageLength;
        private int leadCtaAfterMessages;
        private int ragMaxArticles;
    }
}
