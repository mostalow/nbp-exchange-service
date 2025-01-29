package pl.mostalow.nbp_exchange_service.dto;

import pl.mostalow.nbp_exchange_service.validation.ValidCurrency;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

public record CurrencyExchange(
        @NotNull(message = "Currency code cannot be null")
        @ValidCurrency
        String currencyCode,

        @NotNull(message = "Amount cannot be null")
        @Positive(message = "Amount must be positive")
        BigDecimal amount
) {
}
