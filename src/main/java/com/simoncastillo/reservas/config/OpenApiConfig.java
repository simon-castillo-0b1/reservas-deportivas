package com.simoncastillo.reservas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sports Reservations Management API")
                        .description("API RESTful  para la gestión integral de actividades deportivas, usuarios, reservas concurrentes y control de asistencia.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Simón Castillo")
                                .url("https://github.com/simon-castillo-0b1"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
