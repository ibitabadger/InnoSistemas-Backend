package com.innosistemas.InnoSistemas.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("InnoSistemas API")
                        .version("v1")
                        .description("API REST para el sistema InnoSistemas con autenticación JWT")
                )
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de desarrollo local"),
                        new Server()
                                .url("https://localhost:8080")
                                .description("Servidor de desarrollo local HTTPS"),
                        new Server()
                                .url("https://studious-waffle-pvv4jv9qqx4f9r-8080.app.github.dev")
                                .description("Servidor GitHub Codespaces")
                ))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT Bearer")
                ))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
