package pl.mostalow.nbp_exchange_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExchangeRateCalculateResponse(
        List<ExchangeCurrencyRate> currencyRates,
        BigDecimal totalCost
) {
}
