package pl.mostalow.nbp_exchange_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.mostalow.nbp_exchange_service.dto.ExchangeRateResponse;
import pl.mostalow.nbp_exchange_service.exception.ExchangeRateNotFoundException;
import pl.mostalow.nbp_exchange_service.service.ExchangeRateService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExchangeRateController.class)
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService service;

    @Test
    void getExchangeRate_Success() throws Exception {
        // given
        String currency = "USD";
        String date = "2024-03-15";
        ExchangeRateResponse response = new ExchangeRateResponse(
            currency,
            LocalDate.parse(date),
            BigDecimal.valueOf(3.95)
        );

        when(service.getExchangeRate(eq(currency), any(LocalDate.class)))
            .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/exchange-rate/{currency}/{date}", currency, date))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.currency").value(currency))
            .andExpect(jsonPath("$.rate").value("3.95"));
    }

    @Test
    void getExchangeRate_WhenInvalidDate_ReturnBadRequest() throws Exception {
        // given
        String currency = "USD";
        String invalidDate = "2024-13-45";

        // when & then
        mockMvc.perform(get("/api/exchange-rate/{currency}/{date}", currency, invalidDate))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getExchangeRate_WhenRateNotFound_ReturnNotFound() throws Exception {
        // given
        String currency = "XXX";
        String date = "2024-03-15";

        when(service.getExchangeRate(eq(currency), any(LocalDate.class)))
            .thenThrow(new ExchangeRateNotFoundException("Rate not found"));

        // when & then
        mockMvc.perform(get("/api/exchange-rate/{currency}/{date}", currency, date))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void calculateTotalCost_Success() throws Exception {
        // given
        String requestBody = """
            {
                "currencies": [
                    {"currencyCode": "USD", "amount": 100},
                    {"currencyCode": "EUR", "amount": 200}
                ],
                "date": "2024-03-15"
            }
            """;

        // when & then
        mockMvc.perform(post("/api/exchange-rate/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());
    }

    @Test
    void calculateTotalCost_WhenInvalidRequest_ReturnBadRequest() throws Exception {
        // given
        String invalidRequestBody = """
            {
                "currencies": [],
                "date": "2024-03-15"
            }
            """;

        // when & then
        mockMvc.perform(post("/api/exchange-rate/calculate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequestBody))
            .andExpect(status().isBadRequest());
    }
} 