package com.payshield.payment.config;

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
                        .title("PayShield Payment API")
                        .description("Processes payment transactions. Integrates with Kafka for fraud checks.")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("PayShield Team")
                                .email("dev@payshield.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway (Dev)"),
                        new Server().url("http://localhost:8082").description("Direct Service (Dev)")
                ));
    }
}
