package com.altech.ledger.listener.usecase;

import com.altech.ledger.entity.dto.parity.ParityDtos.CreateRecipientRequest;
import com.altech.ledger.entity.enu.RecipientStatus;
import com.altech.ledger.entity.enu.RecipientTransferChannel;
import com.altech.ledger.usecase.setup.RecipientSetupUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Port of RecipientSetupListener — RECIPIENT_CREATE → create recipient.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.movement.kafka", name = "enabled", havingValue = "true")
public class RecipientSetupListener {
    private static final Logger log = LoggerFactory.getLogger(RecipientSetupListener.class);

    private final ObjectMapper objectMapper;
    private final RecipientSetupUseCase recipientSetupUseCase;

    public RecipientSetupListener(ObjectMapper objectMapper, RecipientSetupUseCase recipientSetupUseCase) {
        this.objectMapper = objectMapper;
        this.recipientSetupUseCase = recipientSetupUseCase;
    }

    @KafkaListener(
        topics = "${ledger.movement.kafka.recipient-created-topic:ledger.recipient.created}",
        groupId = "${ledger.movement.kafka.group-id:ledger-engine-movement}-recipient"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            RecipientTransferChannel channel = RecipientTransferChannel.SWIFT;
            if (node.hasNonNull("transferChannel")) {
                channel = RecipientTransferChannel.valueOf(node.get("transferChannel").asText());
            }
            Long tenantId = node.has("tenantId") ? node.get("tenantId").asLong() : null;
            String metadata = node.has("metadata") ? node.get("metadata").toString() : record.value();
            recipientSetupUseCase.create(new CreateRecipientRequest(
                channel, RecipientStatus.ACTIVE, metadata, tenantId));
            log.info("RECIPIENT_CREATE processed");
        } catch (Exception ex) {
            log.error("Failed RECIPIENT_CREATE: {}", ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }
}
