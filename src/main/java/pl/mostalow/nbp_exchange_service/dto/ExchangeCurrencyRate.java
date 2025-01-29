package pl.mostalow.nbp_exchange_service.dto;

import java.math.BigDecimal;

public record ExchangeCurrencyRate(
        String currencyCode,
        BigDecimal amount,
        BigDecimal rate
) {
}
