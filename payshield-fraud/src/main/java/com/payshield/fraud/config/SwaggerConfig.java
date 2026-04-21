package com.payshield.fraud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayShield Fraud Detection API")
                        .description("AI-powered fraud detection using XGBoost model and rule engine. Scores 0-100.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("PayShield Team")
                                .email("dev@payshield.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway (Dev)"),
                        new Server().url("http://localhost:8083").description("Direct Service (Dev)")
                ));
    }
}
