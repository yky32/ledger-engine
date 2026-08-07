package com.altech.core.kafka;

import com.altech.core.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;


@Slf4j
public class BaseListener {

    public void listener(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        try {
            log.info("-- Topic: [{}]-{} **START**", payload.partition(), payload.topic());
            this.execute(payload, ack);
            log.info("-- Topic: [{}]-{} ***END***", payload.partition(), payload.topic());
        } catch (Exception ex) {
            String errorMessage;
            if (ex instanceof BizException) {
                errorMessage = "-- Error in Topic: ".concat(payload.topic())
                        .concat(", ")
                        .concat(((BizException) ex).getResponse().toString())
                        .concat(" @ ").concat(((BizException) ex).getData().toString());
                log.error("-- Error in Topic: {}, {} @ {}", payload.topic(), ((BizException) ex).getResponse(), ((BizException) ex).getData());
            } else {
                errorMessage = "-- Error in Topic: ".concat(payload.topic()).concat(", ").concat(ex.getMessage());
                log.error("-- Error in Topic: {}, {}", payload.topic(), ex.getMessage());
            }
            elk(errorMessage);
        } finally {
            ack.acknowledge();
            log.info("-- Ack in Topic: {}", payload.topic());
        }
    }

    public void execute(ConsumerRecord<String, String> payload, Acknowledgment ack) {
        // _____ this will be overrides by the normal listener
    }

    public void elk(String message) {

    }
}
