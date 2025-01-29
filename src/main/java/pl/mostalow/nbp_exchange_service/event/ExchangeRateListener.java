package pl.mostalow.nbp_exchange_service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pl.mostalow.nbp_exchange_service.dto.NBPRatesResponse;
import pl.mostalow.nbp_exchange_service.model.ExchangeRate;
import pl.mostalow.nbp_exchange_service.repository.ExchangeRateRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class ExchangeRateListener {

    private final ExchangeRateRepository repository;

    @EventListener
    public void handleNBPResponseEvent(NBPResponseEvent event) {
        NBPRatesResponse nbpRatesResponse = event.nbpRatesResponse();
        String currency = nbpRatesResponse.code();
        NBPRatesResponse.Rate rate = nbpRatesResponse.rates().getFirst();
        LocalDate date = rate.effectiveDate();
        BigDecimal sellRate = rate.ask();

        ExchangeRate exchangeRate = new ExchangeRate(currency, date, sellRate);
        repository.save(exchangeRate);
    }

}
