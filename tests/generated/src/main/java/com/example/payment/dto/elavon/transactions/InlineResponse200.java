package com.example.payment.dto.elavon.transactions;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InlineResponse200 {
    @JsonProperty("transactionId")
    private String transactionid;

    @JsonProperty("status")
    private String status;

    @JsonProperty("authorizationCode")
    private String authorizationcode;
}
