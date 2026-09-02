package com.altech.core.json;

import com.altech.core.constant.enu.Currency;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

/**
 * Writes money {@link BigDecimal} as a JSON string scaled to the sibling currency
 * ({@code Currency.decimalPlaces}). Default 2 dp HALF_UP when no currency is on the bean.
 */
public final class CurrencyAmountSerializer extends JsonSerializer<BigDecimal> {

    private static final List<String> CURRENCY_ACCESSORS = List.of(
        "currency", "getCurrency", "resultCurrency", "getResultCurrency",
        "settlementCurrency", "getSettlementCurrency"
    );

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(Currency.formatAmount(value, resolveCurrency(gen)));
    }

    static Currency resolveCurrency(JsonGenerator gen) {
        JsonStreamContext ctx = gen.getOutputContext();
        while (ctx != null) {
            Currency c = fromBean(ctx.getCurrentValue());
            if (c != null) {
                return c;
            }
            ctx = ctx.getParent();
        }
        return null;
    }

    static Currency fromBean(Object bean) {
        if (bean == null
            || bean instanceof BigDecimal
            || bean instanceof Number
            || bean instanceof CharSequence
            || bean instanceof Enum<?>) {
            return null;
        }
        Currency direct = toCurrency(read(bean, CURRENCY_ACCESSORS));
        if (direct != null) {
            return direct;
        }
        Object coa = read(bean, List.of("coa", "getCoa"));
        if (coa != null && coa != bean) {
            Currency nested = fromBean(coa);
            if (nested != null) {
                return nested;
            }
        }
        Object legs = read(bean, List.of("legs", "getLegs"));
        if (legs instanceof Iterable<?> it) {
            for (Object leg : it) {
                Currency c = fromBean(leg);
                if (c != null) {
                    return c;
                }
            }
        }
        return null;
    }

    private static Currency toCurrency(Object raw) {
        if (raw instanceof Currency c) {
            return c;
        }
        if (raw instanceof String s && !s.isBlank()) {
            try {
                return Currency.get(s);
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Object read(Object bean, List<String> names) {
        Class<?> type = bean.getClass();
        for (String name : names) {
            try {
                Method m = type.getMethod(name);
                return m.invoke(bean);
            } catch (ReflectiveOperationException ignored) {
                // try field
            }
            try {
                Field f = type.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(bean);
            } catch (ReflectiveOperationException ignored) {
                // next name
            }
        }
        return null;
    }
}
