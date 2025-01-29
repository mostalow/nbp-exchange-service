package pl.mostalow.nbp_exchange_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NBPRatesResponse(
        String code,
        List<Rate> rates) {

    public record Rate(
            LocalDate effectiveDate,
            BigDecimal ask) {
    }
}
