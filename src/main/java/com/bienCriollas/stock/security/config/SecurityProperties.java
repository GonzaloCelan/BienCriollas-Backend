package com.bienCriollas.stock.security.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private String jwtSecret = environmentValue("JWT_SECRET");
    private String issuer = environmentOrDefault("JWT_ISSUER", "bien-criollas-api");
    private Duration accessTokenExpiration = environmentDuration("JWT_EXPIRATION", Duration.ofHours(8));
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public Duration getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(Duration accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public BootstrapAdmin getBootstrapAdmin() {
        return bootstrapAdmin;
    }

    public void setBootstrapAdmin(BootstrapAdmin bootstrapAdmin) {
        this.bootstrapAdmin = bootstrapAdmin;
    }

    public static class BootstrapAdmin {
        private String username = environmentValue("ADMIN_USERNAME");
        private String password = environmentValue("ADMIN_PASSWORD");
        private String name = environmentOrDefault("ADMIN_NAME", "Administrador");

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static String environmentValue(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String environmentOrDefault(String name, String defaultValue) {
        String value = environmentValue(name);
        return value == null ? defaultValue : value;
    }

    private static Duration environmentDuration(String name, Duration defaultValue) {
        String value = environmentValue(name);
        if (value == null) {
            return defaultValue;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        try {
            if (normalized.endsWith("h")) {
                return Duration.ofHours(parseUnitlessNumber(normalized));
            }
            if (normalized.endsWith("m")) {
                return Duration.ofMinutes(parseUnitlessNumber(normalized));
            }
            if (normalized.endsWith("s")) {
                return Duration.ofSeconds(parseUnitlessNumber(normalized));
            }
            if (normalized.endsWith("d")) {
                return Duration.ofDays(parseUnitlessNumber(normalized));
            }
            return Duration.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalStateException(
                    name + " debe ser una duración válida, por ejemplo 8h", exception);
        }
    }

    private static long parseUnitlessNumber(String value) {
        return Long.parseLong(value.substring(0, value.length() - 1));
    }
}
