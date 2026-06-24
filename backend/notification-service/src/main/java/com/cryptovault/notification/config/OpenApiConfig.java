package com.cryptovault.notification.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <h3>OpenApiConfig</h3>
 *
 * <p><b>Why it exists:</b> Programmatically configures OpenAPI specifications for the Notification Service.</p>
 * <p><b>Architectural Layer:</b> Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Documents boundaries mapping user security email alerts workflows.</p>
 * <p><b>Scalability Considerations:</b> Highly scalable static model running in-memory with zero disk/db access.</p>
 * <p><b>Interview Talking Points:</b> Integrates Bearer JWT authorization requirements for accessing user alert history logs.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CryptoVault Notification Service API")
                        .description("Enterprise specifications for sending email alerts, template rendering, and delivery tracking.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CryptoVault Support")
                                .email("support@cryptovault.com")
                                .url("https://www.cryptovault.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("BearerJWT"))
                .components(new Components()
                        .addSecuritySchemes("BearerJWT", new SecurityScheme()
                                .name("BearerJWT")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .description("Enter JWT token: Bearer <token>")));
    }
}
