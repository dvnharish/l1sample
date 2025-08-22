package com.client.elavon.transactions;

import java.time.Duration;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException$ServiceUnavailable;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * WebClient for Elavon Transactions operations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionsClient {
    private final WebClient webClient;

    @Value("${elavon.base-url:https://api.elavon.com}")
    private String baseUrl;

    @Value("${elavon.client.timeout:PT30S}")
    private Duration timeout;

    @Value("${elavon.client.max-retries:3}")
    private Integer maxRetries;

    /**
     * Constructor for dependency injection.
     */
    public TransactionsClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * Process a payment transaction
     *
     * @param request @param request The request body
     */
    public Mono<InlineResponse200> processpayment(TransactionsBody request) {
        log.info("Calling processPayment", "/transactions");

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/transactions");
        String uri = uriBuilder.toUriString();

        return webClient.method(HttpMethod.POST);
            .uri(uri);
            .headers(h -> {;
                h.setContentType(MediaType.APPLICATION_JSON);
                h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            });
            .bodyValue(request);
            .retrieve();
            .onStatus(status -> status.isError(), this::handleError);
            .bodyToMono(InlineResponse200.class);
            .timeout(timeout);
            .retryWhen(retrySpec());
            .doOnError(error -> log.error("Error calling processPayment", error));
    }

    /**
     * Get transaction details
     *
     * @param transactionid @param transactionId 
     */
    public Mono<InlineResponse2001> gettransaction(String transactionid) {
        log.info("Calling getTransaction", "/transactions/{transactionId}");

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/transactions/{transactionId}");
        if (transactionid != null) uriBuilder.queryParam("transactionid", transactionid);
        String uri = uriBuilder.toUriString();

        return webClient.method(HttpMethod.GET);
            .uri(uri);
            .headers(h -> {;
                h.setContentType(MediaType.APPLICATION_JSON);
                h.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            });
            .retrieve();
            .onStatus(status -> status.isError(), this::handleError);
            .bodyToMono(InlineResponse2001.class);
            .timeout(timeout);
            .retryWhen(retrySpec());
            .doOnError(error -> log.error("Error calling getTransaction", error));
    }

    private Retry retrySpec() {
        return Retry.backoff(maxRetries, Duration.ofSeconds(1));
            .maxBackoff(Duration.ofSeconds(10));
            .jitter(0.5);
            .filter(throwable -> throwable instanceof WebClientResponseException$ServiceUnavailable);
    }

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class);
            .flatMap(body -> {;
                log.error("API error - Status: {}, Body: {}", response.statusCode(), body);
                return Mono.error(new RuntimeException("API error: " + response.statusCode()));
            });
    }
}
