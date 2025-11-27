package com.ec.bank.payments.infrastructure.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger UI configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentInitiationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Initiation API")
                        .description("REST API for payment order initiation and management, aligned with BIAN Payment Initiation Service Domain")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Banking IT Team")
                                .email("avila_luis_f@yahoo.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://www.hiberus.com/")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.bank.com/v1").description("Production")
                ));
    }
}
