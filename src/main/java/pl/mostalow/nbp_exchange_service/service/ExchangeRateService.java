package pl.mostalow.nbp_exchange_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pl.mostalow.nbp_exchange_service.config.NBPApiProperties;
import pl.mostalow.nbp_exchange_service.dto.CurrencyExchange;
import pl.mostalow.nbp_exchange_service.dto.ExchangeCurrencyRate;
import pl.mostalow.nbp_exchange_service.dto.ExchangeRateCalculateResponse;
import pl.mostalow.nbp_exchange_service.dto.ExchangeRateResponse;
import pl.mostalow.nbp_exchange_service.dto.NBPRatesResponse;
import pl.mostalow.nbp_exchange_service.dto.NBPTableResponse;
import pl.mostalow.nbp_exchange_service.event.NBPResponseEvent;
import pl.mostalow.nbp_exchange_service.exception.ExchangeRateNotFoundException;
import pl.mostalow.nbp_exchange_service.exception.NBPApiException;
import pl.mostalow.nbp_exchange_service.repository.ExchangeRateRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository repository;
    private final WebClient webClient;
    private final ApplicationEventPublisher eventPublisher;
    private final NBPApiProperties nbpApiProperties;

    public ExchangeRateResponse getExchangeRate(String currency, LocalDate date) {
        return repository.findByCurrencyAndDate(currency, date)
                .map(rate -> Mono.just(new ExchangeRateResponse(rate.getCurrency(), rate.getDate(), rate.getRate())))
                .orElseGet(() -> webClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path(nbpApiProperties.getPaths().getTableC())
                                .build(currency, date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                        .retrieve()
                        .onStatus(HttpStatus::is4xxClientError,
                                error -> Mono.error(new ExchangeRateNotFoundException(
                                        "Exchange rate for currency " + currency + " on date " + date + " not found")))
                        .onStatus(HttpStatus::is5xxServerError,
                                error -> Mono.error(new NBPApiException(
                                        "NBP server error while retrieving exchange rate")))
                        .bodyToMono(NBPRatesResponse.class)
                        .map(nbpRatesResponse -> {
                            if (nbpRatesResponse.rates() == null || nbpRatesResponse.rates().isEmpty()) {
                                throw new ExchangeRateNotFoundException(
                                        "No exchange rate data found for currency " + currency + " on date " + date);
                            }
                            eventPublisher.publishEvent(new NBPResponseEvent(nbpRatesResponse));
                            return new ExchangeRateResponse(
                                    nbpRatesResponse.code(),
                                    nbpRatesResponse.rates().getFirst().effectiveDate(),
                                    nbpRatesResponse.rates().getFirst().ask());
                        }))
                .block();
    }

    public ExchangeRateCalculateResponse calculateTotalCost(List<CurrencyExchange> currencies, LocalDate date) {
        NBPTableResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(nbpApiProperties.getPaths().getTableA())
                        .build(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError,
                        error -> Mono.error(new ExchangeRateNotFoundException(
                                format("Exchange rates on date %s not found", date))))
                .onStatus(HttpStatus::is5xxServerError,
                        error -> Mono.error(new NBPApiException(
                                "NBP server error while retrieving exchange rate")))
                .bodyToMono(new ParameterizedTypeReference<List<NBPTableResponse>>() {
                })
                .flatMap(nbpResponse -> {
                    if (nbpResponse.isEmpty()) {
                        return Mono.error(new ExchangeRateNotFoundException(
                                format("No exchange rate data for the date %s", date)));
                    }
                    return Mono.just(nbpResponse.getFirst());
                })
                .block();
        return calculateTotalCost(currencies, response);
    }

    private ExchangeRateCalculateResponse calculateTotalCost(List<CurrencyExchange> currencies, NBPTableResponse response) {
        if (response == null || response.rates() == null || response.rates().isEmpty()) {
            throw new ExchangeRateNotFoundException("No data on exchange rates in response from NBP API");
        }
        Map<String, BigDecimal> rateMap = response.rates().stream()
                .collect(Collectors.toMap(NBPTableResponse.Rate::code, NBPTableResponse.Rate::mid));
        List<ExchangeCurrencyRate> exchangeCurrencyRates = currencies.stream()
                .filter(currency -> rateMap.containsKey(currency.currencyCode()))
                .map(currency -> {
                    BigDecimal rate = rateMap.get(currency.currencyCode());
                    return new ExchangeCurrencyRate(
                            currency.currencyCode(),
                            currency.amount(),
                            rate);
                })
                .toList();
        if (exchangeCurrencyRates.isEmpty()) {
            throw new ExchangeRateNotFoundException("No exchange rates found for any of the provided currencies");
        }
        BigDecimal totalCost = exchangeCurrencyRates.stream()
                .map(currencyRate -> currencyRate.amount().multiply(currencyRate.rate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ExchangeRateCalculateResponse(exchangeCurrencyRates, totalCost);
    }
}
