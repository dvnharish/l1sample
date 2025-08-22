package com.dto.elavon.transactions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InlineResponse2001 {
    @JsonProperty("transactionId")
    private String transactionid;

    @JsonProperty("status")
    private String status;

    @JsonProperty("total")
    @Valid
    private TransactionsTotal total;

    @JsonProperty("createdAt")
    private OffsetDateTime createdat;
}
