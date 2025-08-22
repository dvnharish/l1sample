package com.example.payment.mapper.elavon.transactions;

import com.elavon.dto.ProcesspaymentRequest;
import com.elavon.dto.ProcesspaymentResponse;
import com.legacy.converge.dto.ConvergeProcesspaymentRequest;
import com.legacy.converge.dto.ConvergeProcesspaymentResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between Converge and Elavon models.
 * Operation: processPayment
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProcesspaymentMapper {
    /**
     * Convert Converge XML request to Elavon JSON request.
     */
    @Mapping(
            source = "amount",
            target = "total.amount",
            qualifiedByName = "formatAmount"
    )
    @Mapping(
            source = "currency",
            target = "total.currencyCode"
    )
    @Mapping(
            source = "cardExpiry",
            target = "card.expirationMonth",
            qualifiedByName = "extractExpiryMonth"
    )
    @Mapping(
            source = "cardExpiry",
            target = "card.expirationYear",
            qualifiedByName = "extractExpiryYear"
    )
    @Mapping(
            source = "cardNumber",
            target = "card.number",
            qualifiedByName = "maskCardNumber"
    )
    ProcesspaymentRequest toElavonRequest(ConvergeProcesspaymentRequest convergeRequest);

    /**
     * Convert Elavon JSON response to Converge XML response format.
     * This is used to maintain backward compatibility.
     */
    @Mapping(
            source = "status",
            target = "result",
            qualifiedByName = "mapStatusToResult"
    )
    @Mapping(
            source = "authorizationCode",
            target = "approvalCode"
    )
    @Mapping(
            source = "transactionId",
            target = "txnId"
    )
    @Mapping(
            source = "createdAt",
            target = "timestamp",
            qualifiedByName = "formatTimestamp"
    )
    ConvergeProcesspaymentResponse toConvergeResponse(ProcesspaymentResponse elavonResponse);

    @Named
    default String formatAmount(Long amountInCents) {
        if (amountInCents == null) return null;
        return BigDecimal.valueOf(amountInCents).divide(BigDecimal.valueOf(100)).toPlainString();
    }

    @Named
    default Long parseAmount(String amountStr) {
        if (amountStr == null) return null;
        return BigDecimal.valueOf(amountStr).multiply(BigDecimal.valueOf(100)).longValue();
    }

    @Named
    default String extractExpiryMonth(String expiry) {
        if (expiry == null || expiry.length() < 2) return null;
        return expiry.substring(0, 2);
    }

    @Named
    default String extractExpiryYear(String expiry) {
        if (expiry == null || expiry.length() < 4) return null;
        return "20" + expiry.substring(2, 4);
    }

    @Named
    default String formatTimestamp(OffsetDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * PCI Compliance: Never include full card number.
     * Card should be tokenized before reaching this point.
     */
    @Named
    default String maskCardNumber(String cardNumber) {
        // Never pass full PAN - this should already be tokenized;
        return null;
    }

    @Named
    default String getLastFourDigits(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return null;
        return cardNumber.substring(cardNumber.length() - 4);
    }

    @Named
    default String mapStatusToResult(String status) {
        return switch (status) {
            case "APPROVED" -> "APPROVAL";
            case "DECLINED" -> "DECLINE";
            case "PENDING" -> "PENDING";
            case "CANCELLED" -> "VOID";
            case "REFUNDED" -> "REFUND";
            default -> "ERROR";
        }
    }

    @Named
    default String mapResultToStatus(String result) {
        return switch (result) {
            case "APPROVAL" -> "APPROVED";
            case "DECLINE" -> "DECLINED";
            case "PENDING" -> "PENDING";
            case "VOID" -> "CANCELLED";
            case "REFUND" -> "REFUNDED";
            default -> "FAILED";
        }
    }
}
