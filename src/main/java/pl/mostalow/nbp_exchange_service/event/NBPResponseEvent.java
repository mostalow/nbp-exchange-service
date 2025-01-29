package pl.mostalow.nbp_exchange_service.event;

import pl.mostalow.nbp_exchange_service.dto.NBPRatesResponse;

public record NBPResponseEvent(NBPRatesResponse nbpRatesResponse) {}

