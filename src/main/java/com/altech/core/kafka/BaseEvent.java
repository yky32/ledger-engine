package com.altech.core.kafka;

import com.altech.core.utils.RandomHashGenerator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseEvent {
    protected String requestId = "T".concat(Objects.requireNonNull(RandomHashGenerator.generateRandomHash(32)));
    protected String eventName;
    protected String webhookUrl;

    protected KafkaControl consumerControl;
    protected KafkaControl producerControl;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class KafkaControl {
        private Instant targetEndAt;
        private long restIntervalInMilliseconds;
    }
}
