package com.cryptovault.auth.config;

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
 * <p><b>Why it exists:</b> Programmatically configures OpenAPI specifications for the Authentication Service.</p>
 * <p><b>Architectural Layer:</b> Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Exposes secure boundaries for user registration and authentication workflows.</p>
 * <p><b>Scalability Considerations:</b> Highly scalable metadata model with zero runtime processing overhead.</p>
 * <p><b>Interview Talking Points:</b> Configures Bearer JWT security scheme globally to expose authorization buttons on API document dashboards.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CryptoVault Auth Service API")
                        .description("Enterprise specifications for user registration, authentication, and token parsing services.")
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
