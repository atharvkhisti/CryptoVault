# Swagger/OpenAPI Integration Walkthrough & Architecture Reference

This document provides a comprehensive technical walkthrough of the **Swagger/OpenAPI 3.0** documentation integration across the **CryptoVault** platform, serving as an architectural blueprint and an interview prep reference.

---

## 1. Why OpenAPI Integration Exists
In a microservices-based architecture like CryptoVault, maintaining separate documentation for each microservice leads to fragmented developer experiences. 
- **Centralized Registry:** Aggregates documentation of all downline microservices (`auth-service`, `wallet-service`, `transaction-service`, `notification-service`, `risk-service`, `audit-service`, `kyc-service`) into a single dashboard.
- **Contract-First Consistency:** Promotes standard interfaces where payload schemas, response structures, and HTTP statuses are clearly defined.
- **Interactive Sandbox:** Provides an operational dashboard for developers to test and query endpoints directly without requiring external tools like Postman.

---

## 2. Dependency Integration Strategy
To enable OpenAPI 3.0 documentation generation consistently across the stack:
- **Shared Library (`common-lib`):** Added the compile-time `swagger-annotations-jakarta` dependency to [common-lib/pom.xml](file:///d:/CryptoVault/backend/common-lib/pom.xml). This allows shared DTOs (like `ApiResponse.java`) to be decorated with `@Schema` without introducing servlet dependencies into downstream layers.
- **Gateway & Microservices:** Added `springdoc-openapi-starter-webmvc-ui` to all backend POM files. Because the API Gateway runs on standard Servlet-based Spring Cloud Gateway MVC (not reactive WebFlux), the same WebMvc-based starter is utilized uniformly across all 8 modules.

---

## 3. Centralized API Gateway Aggregation
The **API Gateway** acts as the single point of entry for Swagger UI resources:
- **Central Dashboard:** Accessing `http://localhost:8080/swagger-ui/index.html` serves a central dashboard UI.
- **Dynamic Routing:** Configured Swagger UI group configurations via `springdoc.swagger-ui.urls` mapping:
  - Auth Service: `/api/auth/v3/api-docs`
  - Wallet Service: `/api/wallets/v3/api-docs`
  - Transaction Service: `/api/transactions/v3/api-docs`
  - Notification Service: `/api/notifications/v3/api-docs`
  - Risk Service: `/api/risk/v3/api-docs`
  - Audit Service: `/api/audit/v3/api-docs`
  - KYC Service: `/api/kyc/v3/api-docs`
- When selected in the Swagger UI dropdown, the Gateway routes the documentation requests downstream to fetch `/api/[service]/v3/api-docs`, dynamically rendering the target service specifications.

---

## 4. OpenAPI Global Security Scheme
All protected paths in the CryptoVault platform require a bearer JWT token verified at the API Gateway. To support this boundary in Swagger UI:
- **`BearerAuth` Scheme:** Every microservice defines a global security scheme in its `OpenApiConfig.java`:
  ```java
  @Configuration
  public class OpenApiConfig {
      @Bean
      public OpenAPI customOpenAPI() {
          return new OpenAPI()
              .info(new Info().title("CryptoVault API").version("1.0.0"))
              .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
              .components(new Components()
                  .addSecuritySchemes("BearerAuth", new SecurityScheme()
                      .name("BearerAuth")
                      .type(SecurityScheme.Type.HTTP)
                      .scheme("bearer")
                      .bearerFormat("JWT")));
      }
  }
  ```
- **Authorized Sandboxing:** Registers an "Authorize" button in Swagger UI. Developers authenticate via `/api/auth/login`, paste the raw token in Swagger UI, and the UI automatically attaches `Authorization: Bearer <token>` to subsequent mock requests.

---

## 5. Swagger UI Customizations & Properties
Swagger UI behaviors are customized via Spring properties in each module's `application.properties`:
- `springdoc.swagger-ui.tagsSorter=alpha`: Sorts endpoint controller tags alphabetically.
- `springdoc.swagger-ui.operationsSorter=alpha`: Sorts operations within a tag alphabetically.
- `springdoc.swagger-ui.displayRequestDuration=true`: Displays execution latency directly in the output console for profiling.
- `springdoc.swagger-ui.showOperationIds=true`: Shows the underlying controller method operation IDs.

---

## 6. Controller & DTO Decoration Patterns
Consistent Swagger annotations decorate all microservices controllers and DTOs:
- **Controllers:** Classes are annotated with `@Tag` defining tag names and roles. Methods are annotated with `@Operation` for summaries and details, along with `@Parameter` for method inputs.
- **Envelope Response Wrappers:** To prevent annotation namespace conflicts with the platform's custom envelope class `ApiResponse`, operations specify the fully qualified name `@io.swagger.v3.oas.annotations.responses.ApiResponse` mapping typical error statuses (400, 401, 403, 404, 409, 500) and schemas:
  ```java
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "400", 
      description = "Validation parameter failure", 
      content = @Content(schema = @Schema(implementation = ApiResponse.class))
  )
  ```
- **DTOs:** Request/response payloads are annotated with `@Schema` describing parameters, required modes, and constraints.

---

## 7. Security Configurations & Route Bypasses
To allow client browsers to access Swagger UI without credentials checks:
- **Gateway Ingress:** Updated `GatewayAuthenticationFilter.java` to bypass public path patterns (`/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`, `/api/*/v3/api-docs`).
- **Spring Security Rules:** Configured `SecurityConfig.java` in all services to permit public access to documentation routes:
  ```java
  .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/*/v3/api-docs").permitAll()
  ```
- **Interceptor Pass-Through:** In-service JWT context filters (like `JwtUserInterceptorFilter`) allow documentation request paths to pass through safely on missing headers.

---

## 8. Verification & Testing Strategy
Our verification pipeline guarantees that the OpenAPI integration does not regress system stability:
1. **Compilation Checks:** Run `mvn clean test-compile` on the parent workspace to confirm Swagger references resolve.
2. **Security Integration Tests:** Asserts that `/v3/api-docs` and `/swagger-ui/index.html` return HTTP `200 OK` anonymously, while standard business APIs continue to return HTTP `401 Unauthorized` without a valid token.
3. **Execution Sandbox:** Accessing `http://localhost:8080/swagger-ui/index.html` dynamically updates documentation scopes across all 7 services.

---

## 9. Future Enhancements
- **Asymmetric Signature Integration (JWKS):** Migrate downstream mock token authorization flows to utilize public-key validation.
- **OpenAPI Mock Servers:** Integrate Prism mock servers automatically generated from compiled `/v3/api-docs` specs, letting frontend developers write code against local endpoints before microservices are fully deployed.

---

## 10. Interview Talking Points
1. **Unified Gateway Aggregation vs. Direct Service Documentation:** *"We aggregate all microservices documentation at the API Gateway boundary. This hides the microservice topology and ports from external users, allowing developers to query, authorize, and sand-box standard endpoints in one unified dashboard."*
2. **Resolving Classpath Annotation Conflicts:** *"Because our shared library distributes a custom `ApiResponse` class envelope, standardizing on Swagger's `@ApiResponse` would cause class namespace conflicts. We resolved this by explicitly qualifying Swagger's annotation as `@io.swagger.v3.oas.annotations.responses.ApiResponse` in controllers, maintaining compiler safety."*
3. **Why jakarta swagger annotations in common-lib?** *"By including only the `swagger-annotations-jakarta` library (rather than SpringDoc MVC UI) in the shared common library, we prevent transitively pulling servlet container classes or reactive conflict libraries into modules that do not need them, preserving a clean classpath boundary."*
