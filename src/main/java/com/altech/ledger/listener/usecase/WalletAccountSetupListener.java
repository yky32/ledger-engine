package com.altech.ledger.listener.usecase;

import com.altech.ledger.usecase.wallet.CreateWalletUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.parity.LedgerWalletDtos;

/**
 * WalletAccountSetupListener — WALLET_CREATED → create accounts + wallet.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.movement.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class WalletAccountSetupListener {
    private static final Logger log = LoggerFactory.getLogger(WalletAccountSetupListener.class);

    private final ObjectMapper objectMapper;
    private final CreateWalletUseCase createWalletUseCase;

    @KafkaListener(
        topics = "${ledger.movement.kafka.wallet-created-topic:ledger.wallet.created}",
        groupId = "${ledger.movement.kafka.group-id:ledger-engine-movement}-wallet"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            JsonNode node = objectMapper.readTree(record.value());
            String ownerId = text(node, "ownerId", "userId", "walletExtIdentifier");
            String currency = text(node, "mainCurrency", "currency");
            if (currency == null) currency = "USD";
            String extId = text(node, "walletExtIdentifier", "extIdentifier");
            String extType = text(node, "walletExtType", "extType");
            List<String> extras = new ArrayList<>();
            if (node.has("accountCurrencies") && node.get("accountCurrencies").isArray()) {
                node.get("accountCurrencies").forEach(c -> extras.add(c.asText()));
            }
            LedgerWalletDtos.WithBalancesResponse created = createWalletUseCase.executeFull(
                ownerId, currency, extras, extId, extType);
            log.info("WALLET_CREATED processed walletId={}", created.id());
        } catch (Exception ex) {
            log.error("Failed WALLET_CREATED: {}", ex.getMessage(), ex);
            throw new IllegalStateException(ex);
        }
    }

    private static String text(JsonNode node, String... fields) {
        for (String f : fields) {
            if (node.hasNonNull(f)) return node.get(f).asText();
        }
        return null;
    }
}
