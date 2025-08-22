package com.example.payment.dto.elavon.transactions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionsBody {
    @JsonProperty("total")
    @NotNull
    @Valid
    private TransactionsTotal total;

    @JsonProperty("card")
    @NotNull
    @Valid
    private TransactionsCard card;
}
