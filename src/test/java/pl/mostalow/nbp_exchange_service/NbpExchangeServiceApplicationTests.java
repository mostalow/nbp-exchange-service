package pl.mostalow.nbp_exchange_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import pl.mostalow.nbp_exchange_service.controller.ExchangeRateController;
import pl.mostalow.nbp_exchange_service.service.ExchangeRateService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NbpExchangeServiceApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ExchangeRateController exchangeRateController;

	@Autowired
	private ExchangeRateService exchangeRateService;

	@Test
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
		assertThat(exchangeRateController).isNotNull();
		assertThat(exchangeRateService).isNotNull();
	}
}