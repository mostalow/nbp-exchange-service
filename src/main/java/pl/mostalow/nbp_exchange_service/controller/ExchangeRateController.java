package pl.mostalow.nbp_exchange_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.mostalow.nbp_exchange_service.dto.ExchangeRateCalculateResponse;
import pl.mostalow.nbp_exchange_service.dto.ExchangeRateResponse;
import pl.mostalow.nbp_exchange_service.dto.TotalCostRequest;
import pl.mostalow.nbp_exchange_service.service.ExchangeRateService;

import javax.validation.Valid;
import java.time.DateTimeException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/exchange-rate")
@Validated
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/{currency}/{date}")
    public ResponseEntity<ExchangeRateResponse> getExchangeRate(@PathVariable String currency, @PathVariable String date) {
        try {
            LocalDate parsedDate = LocalDate.parse(date);
            ExchangeRateResponse rate = exchangeRateService.getExchangeRate(currency, parsedDate);
            return ResponseEntity.ok(rate);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid date format. Required format: YYYY-MM-DD");
        }
    }

    @PostMapping("/calculate")
    public ResponseEntity<ExchangeRateCalculateResponse> calculateTotalCost(@Valid @RequestBody TotalCostRequest request) {
        ExchangeRateCalculateResponse response = exchangeRateService.calculateTotalCost(request.currencies(), request.date());
        return ResponseEntity.ok(response);
    }
}
