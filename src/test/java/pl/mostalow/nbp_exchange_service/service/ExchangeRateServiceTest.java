package pl.mostalow.nbp_exchange_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import pl.mostalow.nbp_exchange_service.config.NBPApiProperties;
import pl.mostalow.nbp_exchange_service.dto.CurrencyExchange;
import pl.mostalow.nbp_exchange_service.dto.ExchangeRateCalculateResponse;
import pl.mostalow.nbp_exchange_service.dto.ExchangeRateResponse;
import pl.mostalow.nbp_exchange_service.dto.NBPRatesResponse;
import pl.mostalow.nbp_exchange_service.dto.NBPTableResponse;
import pl.mostalow.nbp_exchange_service.event.NBPResponseEvent;
import pl.mostalow.nbp_exchange_service.exception.ExchangeRateNotFoundException;
import pl.mostalow.nbp_exchange_service.model.ExchangeRate;
import pl.mostalow.nbp_exchange_service.repository.ExchangeRateRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository repository;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private NBPApiProperties nbpApiProperties;

    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        service = new ExchangeRateService(repository, webClient, eventPublisher, nbpApiProperties);
    }

    @Test
    void getExchangeRate_WhenRateExists_ReturnFromRepository() {
        // given
        String currency = "USD";
        LocalDate date = LocalDate.of(2024, 3, 15);
        BigDecimal rate = BigDecimal.valueOf(3.95);
        ExchangeRate exchangeRate = new ExchangeRate(currency, date, rate);

        when(repository.findByCurrencyAndDate(currency, date))
                .thenReturn(Optional.of(exchangeRate));

        // when
        ExchangeRateResponse response = service.getExchangeRate(currency, date);

        // then
        assertThat(response.currency()).isEqualTo(currency);
        assertThat(response.date()).isEqualTo(date);
        assertThat(response.rate()).isEqualTo(rate);
        verify(webClient, never()).get();
    }

    @Test
    void getExchangeRate_WhenRateNotExists_FetchFromNBPApi() {
        // given
        String currency = "EUR";
        LocalDate date = LocalDate.of(2024, 3, 15);
        NBPRatesResponse nbpResponse = createNBPRatesResponse();

        mockWebClientForGetExchangeRate(nbpResponse);

        // when
        ExchangeRateResponse response = service.getExchangeRate(currency, date);

        // then
        assertThat(response).isNotNull();
        verify(eventPublisher).publishEvent(any(NBPResponseEvent.class));
    }

    @Test
    void calculateTotalCost_Success() {
        // given
        LocalDate date = LocalDate.of(2024, 3, 15);
        List<CurrencyExchange> currencies = List.of(
                new CurrencyExchange("USD", BigDecimal.TEN),
                new CurrencyExchange("EUR", BigDecimal.valueOf(20))
        );
        NBPTableResponse nbpResponse = createNBPTableResponse();

        mockWebClientForCalculateTotalCost(List.of(nbpResponse));

        // when
        ExchangeRateCalculateResponse response = service.calculateTotalCost(currencies, date);

        // then
        assertThat(response).isNotNull();
        assertThat(response.currencyRates()).hasSize(2);

        BigDecimal expectedTotalCost = BigDecimal.TEN.multiply(BigDecimal.valueOf(3.95))
                .add(BigDecimal.valueOf(20).multiply(BigDecimal.valueOf(4.32)));
        assertThat(response.totalCost()).isEqualByComparingTo(expectedTotalCost);
    }

    @Test
    void calculateTotalCost_WhenNoRatesFound_ThrowException() {
        // given
        LocalDate date = LocalDate.of(2024, 3, 15);
        List<CurrencyExchange> currencies = List.of(
                new CurrencyExchange("INVALID", BigDecimal.TEN)
        );
        NBPTableResponse nbpResponse = createEmptyNBPTableResponse();

        mockWebClientForCalculateTotalCost(List.of(nbpResponse));

        // when & then
        assertThrows(ExchangeRateNotFoundException.class,
                () -> service.calculateTotalCost(currencies, date));
    }

    private void mockWebClientForGetExchangeRate(NBPRatesResponse nbpResponse) {
        mockWebClient();
        when(requestHeadersUriSpec.uri(any(Function.class)))
                .thenReturn(requestHeadersSpec);
        when(responseSpec.bodyToMono(NBPRatesResponse.class))
                .thenReturn(Mono.just(nbpResponse));
    }

    private void mockWebClientForCalculateTotalCost(List<NBPTableResponse> response) {
        mockWebClient();
        when(requestHeadersUriSpec.uri(any(Function.class)))
                .thenReturn(requestHeadersSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(response));
    }

    private void mockWebClient() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }

    private NBPRatesResponse createNBPRatesResponse() {
        return new NBPRatesResponse(
                "USD",
                List.of(new NBPRatesResponse.Rate(
                        LocalDate.now(),
                        BigDecimal.valueOf(3.95)
                ))
        );
    }

    private NBPTableResponse createNBPTableResponse() {
        return new NBPTableResponse(
                LocalDate.now(),
                List.of(
                        new NBPTableResponse.Rate("USD", BigDecimal.valueOf(3.95)),
                        new NBPTableResponse.Rate("EUR", BigDecimal.valueOf(4.32))
                )
        );
    }

    private NBPTableResponse createEmptyNBPTableResponse() {
        return new NBPTableResponse(LocalDate.now(), List.of());
    }
} 