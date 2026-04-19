package com.payshield.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableRetry
public class PayShieldNotificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayShieldNotificationApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
