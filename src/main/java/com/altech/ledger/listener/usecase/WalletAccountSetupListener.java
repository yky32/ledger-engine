package com.altech.ledger.listener.usecase;

import com.altech.ledger.usecase.wallet.CreateWalletUseCase;
import com.altech.core.utils.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import com.altech.ledger.entity.dto.response.GetLedgerWalletResponseDto;

/**
 * WalletAccountSetupListener — WALLET_CREATED → create accounts + wallet.
 */
@Component
@ConditionalOnProperty(prefix = "ledger.movement.kafka", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class WalletAccountSetupListener {
    private static final Logger log = LoggerFactory.getLogger(WalletAccountSetupListener.class);

    private final CreateWalletUseCase createWalletUseCase;

    @KafkaListener(
        topics = "${ledger.movement.kafka.wallet-created-topic:ledger.wallet.created}",
        groupId = "${ledger.movement.kafka.group-id:ledger-engine-movement}-wallet"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            JsonNode node = JSONUtil.readTree(record.value());
            String ownerId = text(node, "ownerId", "userId", "walletAssociatedIdentifier", "associatedIdentifier");
            String currency = text(node, "mainCurrency", "currency");
            if (currency == null) currency = "USD";
            List<String> extras = new ArrayList<>();
            if (node.has("accountCurrencies") && node.get("accountCurrencies").isArray()) {
                node.get("accountCurrencies").forEach(c -> extras.add(c.asText()));
            }
            GetLedgerWalletResponseDto created = createWalletUseCase.executeFull(
                ownerId, currency, extras);
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
