package com.payshield.reconciliation.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.RecordInterceptor;

@Configuration
@Slf4j
public class KafkaLoggingConfig {

    @Bean
    public RecordInterceptor<String, Object> kafkaRecordInterceptor() {
        return (record, consumer) -> {
            log.info("KAFKA IN | topic={} | partition={} | offset={} | key={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key());
            return record;
        };
    }
}