package pl.mostalow.nbp_exchange_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRateResponse(
        String currency,
        LocalDate date,
        BigDecimal rate) {
}
