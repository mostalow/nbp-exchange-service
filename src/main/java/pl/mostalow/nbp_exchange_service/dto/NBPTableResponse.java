package pl.mostalow.nbp_exchange_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record NBPTableResponse(
        LocalDate effectiveDate,
        List<Rate> rates) {

    public record Rate(
            String code,
            BigDecimal mid
    ){}
}
