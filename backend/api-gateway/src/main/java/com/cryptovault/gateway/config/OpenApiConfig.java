package com.cryptovault.gateway.config;

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
 * <p><b>Why it exists:</b> Programmatically configures centralized OpenAPI/Swagger specifications at the Gateway entry point.</p>
 * <p><b>Architectural Layer:</b> Configuration Layer.</p>
 * <p><b>Design Patterns Used:</b> Builder Pattern.</p>
 * <p><b>Financial Compliance Relevance:</b> Exposes documentation describing client security boundaries and authentication parameters.</p>
 * <p><b>Scalability Considerations:</b> Highly scalable as metadata serves static UI render contexts with zero downstream processing.</p>
 * <p><b>Interview Talking Points:</b> Configures Bearer JWT authentication globally so that frontend developer teams can authorize requests directly within the Swagger UI dashboard.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CryptoVault API Gateway")
                        .description("Centralized API Gateway entry point aggregating all CryptoVault microservices specs.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("CryptoVault Engineering")
                                .email("dev@cryptovault.com")
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
