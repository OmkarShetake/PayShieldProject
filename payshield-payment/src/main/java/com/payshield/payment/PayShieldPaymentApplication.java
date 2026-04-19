package com.payshield.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableKafka
@EnableAsync
public class PayShieldPaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayShieldPaymentApplication.class, args);
    }
}
