package com.example.library.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Library Management API")
                        .version("1.0.0")
                        .description("""
                                ## Library Management API
                                
                                A complete REST API featuring:
                                - **CRUD** for Authors and Books
                                - **Pagination, Sorting, Filtering** via Spring Data JPA
                                - **Custom Queries** with JPQL, Native SQL and Specifications
                                - **Cache** with Caffeine
                                - **JWT Authentication** via Spring Security
                                - **HATEOAS** links on all responses
                                
                                ### Getting started
                                1. Register at `POST /api/v1/auth/register`
                                2. Login at `POST /api/v1/auth/login` to get your JWT token
                                3. Click **Authorize** and paste `Bearer <your-token>`
                                4. Explore the endpoints!
                                """)
                        .contact(new Contact()
                                .name("Library API Team")
                                .email("api@library.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste your JWT token here (without the 'Bearer ' prefix)")));
    }
}
