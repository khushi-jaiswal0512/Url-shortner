package com.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Springdoc OpenAPI / Swagger UI configuration.
 * Accessible at: /swagger-ui/index.html
 */
@Configuration
public class SwaggerConfig {

    @Value("${app.base-url}")
    private String baseUrl;

    @Bean
    public OpenAPI urlShortenerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("URL Shortener API")
                        .description("Production-ready URL Shortener service inspired by Alex Xu's System Design Interview. "
                                + "Supports URL shortening, 301 redirection, analytics, and soft-deletion.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("URL Shortener")
                                .url(baseUrl))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Current Environment")));
    }
}
