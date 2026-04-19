package com.payshield.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableKafka
public class PayShieldFraudApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayShieldFraudApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
