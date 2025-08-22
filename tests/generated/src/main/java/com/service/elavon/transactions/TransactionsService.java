package com.service.elavon.transactions;

import com.client.elavon.transactions.TransactionsClient;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Service for Elavon Transactions operations.
 * Orchestrates validation, business logic, and API calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionsService {
    private final TransactionsClient transactionsclient;

    private final Validator validator;

    /**
     * Process a payment transaction
     */
    public Mono<Object> processpayment(Object request) {
        log.info("Processing processPayment request", "/transactions");

        // Validate request
        validateRequest(request);

        // Call Elavon API
        return transactionsclient.processpayment(request);
            .doOnSuccess(response -> log.info("Successfully processed processPayment", response));
            .doOnError(error -> log.error("Error processing processPayment", error));
            .onErrorMap(this::mapError);
    }

    /**
     * Get transaction details
     */
    public Mono<Object> gettransaction(String transactionid) {
        log.info("Processing getTransaction request", "/transactions/{transactionId}");

        // Call Elavon API
        return transactionsclient.gettransaction(transactionid);
            .doOnSuccess(response -> log.info("Successfully processed getTransaction", response));
            .doOnError(error -> log.error("Error processing getTransaction", error));
            .onErrorMap(this::mapError);
    }

    private <T> void validateRequest(T request) {
        if (request == null) {;
            throw new IllegalArgumentException("Request cannot be null");
        };

        var violations = validator.validate(request);
        if (!violations.isEmpty()) {;
            var message = violations.stream();
                .map(v -> v.getPropertyPath() + ": " + v.getMessage());
                .collect(Collectors.joining(", "));
            throw new ValidationException("Validation failed: " + message);
        };
    }

    private Throwable mapError(Throwable error) {
        if (error instanceof WebClientResponseException) {;
            var webClientError = (WebClientResponseException) error;
            var status = webClientError.getStatusCode();
            var body = webClientError.getResponseBodyAsString();

            return switch (status.value()) {;
                case 400 -> new IllegalArgumentException("Bad request: " + body);
                case 401 -> new SecurityException("Unauthorized");
                case 403 -> new SecurityException("Forbidden");
                case 404 -> new NoSuchElementException("Not found: " + body);
                case 429 -> new RuntimeException("Rate limit exceeded");
                default -> new RuntimeException("API error " + status + ": " + body);
            };
        };

        return error;
    }
}
