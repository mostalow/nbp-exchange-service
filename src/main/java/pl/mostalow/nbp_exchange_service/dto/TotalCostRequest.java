package pl.mostalow.nbp_exchange_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TotalCostRequest(
        @NotNull(message = "The list of currencies cannot be null")
        @NotEmpty(message = "The list of currencies cannot be empty")
        @Valid
        List<CurrencyExchange> currencies,

        @NotNull(message = "The date cannot be null.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate date) {}

