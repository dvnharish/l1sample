package com.example.payment.controller.elavon.transactions;

import com.example.payment.service.elavon.transactions.TransactionsService;
import io.swagger.v3.oas.models.parameters.RequestBody;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for Elavon Transactions operations.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/elavon/transactions")
public class TransactionsController {
    private final TransactionsService transactionsservice;

    /**
     * Process a payment transaction
     */
    @PostMapping(
            value = "/transactions",
            consumes = "application/json",
            produces = "application/json"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<InlineResponse200>> processpayment(
            @Valid @RequestBody @NotNull TransactionsBody request) {
        log.info("Handling POST request", "/transactions");

        return transactionsservice.processpayment(request);
            .map(ResponseEntity::ok);
            .doOnSuccess(response -> log.info("Successfully handled processPayment"));
            .doOnError(error -> log.error("Error handling processPayment", error));
    }

    /**
     * Get transaction details
     */
    @GetMapping(
            value = "/transactions/{transactionId}",
            produces = "application/json"
    )
    public Mono<ResponseEntity<InlineResponse2001>> gettransaction(
            @PathVariable("transactionId") @NotNull String transactionid) {
        log.info("Handling GET request", "/transactions/{transactionId}");

        return transactionsservice.gettransaction(transactionid);
            .map(ResponseEntity::ok);
            .doOnSuccess(response -> log.info("Successfully handled getTransaction"));
            .doOnError(error -> log.error("Error handling getTransaction", error));
    }

    @ExceptionHandler
    public ResponseEntity<Map> handleValidationException(ValidationException ex) {
        log.error("Validation error", ex);
        var error = Map.of("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }
}
