package de.predic8.workshop.checkout.service;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import de.predic8.workshop.checkout.dto.Basket;
import de.predic8.workshop.checkout.dto.Item;
import de.predic8.workshop.checkout.dto.Stock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static net.logstash.logback.marker.Markers.appendEntries;


@Service
public class CheckoutService {
	private static final Logger log = LoggerFactory.getLogger(CheckoutService.class);



	private final RestTemplate rest;

	public CheckoutService(RestTemplate restTemplate) {
		this.rest = restTemplate;
	}

	@HystrixCommand(fallbackMethod = "fallback")
	public boolean areArticlesAvailable(Basket basket) {

		return basket.getItems().stream().parallel().allMatch( item -> {
				Stock stock = rest.getForObject( "http://stock/stocks/{uuid}", Stock.class, item.getArticleId());
				log.info("checking: " + item.getArticleId() + " (" + item.getQuantity() + "/" + stock.getQuantity() + ")");
				return stock.getQuantity()>=item.getQuantity();
			}
		);
	}

	public boolean fallback(Basket basket, Throwable t) {
	    Map<String, Object> entries = new HashMap<>();
	    entries.put("fallback", t);
	    log.error(appendEntries(entries),"Hystrix");
        return true;
    }
}
