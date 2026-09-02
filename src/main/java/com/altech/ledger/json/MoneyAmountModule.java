package com.altech.ledger.json;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * HTTP JSON: money fields serialize as currency-scaled strings.
 * Request bodies still accept number or string.
 */
public final class MoneyAmountModule extends SimpleModule {

    static final Set<String> MONEY_PROPERTIES = Set.of(
        "amount",
        "ledgerBalance",
        "availableBalance",
        "points",
        "fxConvertedBalance",
        "liveLedgerBalance",
        "liveAvailableBalance",
        "minAmount"
    );

    public MoneyAmountModule() {
        super("MoneyAmountModule");
        addDeserializer(BigDecimal.class, new CurrencyAmountDeserializer());
        setSerializerModifier(new Modifier());
    }

    private static final class Modifier extends BeanSerializerModifier {
        @SuppressWarnings("rawtypes")
        private static final JsonSerializer SER = new CurrencyAmountSerializer();

        @Override
        public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties
        ) {
            for (BeanPropertyWriter w : beanProperties) {
                if (MONEY_PROPERTIES.contains(w.getName())
                    && w.getType() != null
                    && w.getType().isTypeOrSubTypeOf(BigDecimal.class)) {
                    w.assignSerializer(SER);
                }
            }
            return beanProperties;
        }
    }
}
