package com.example.converge.dto.request;

import jakarta.validation.constraints.*;
import com.example.converge.validation.Luhn;

public class SaleRequest {

    @NotBlank
    @Pattern(regexp = "^(?!0+(?:\\.0+)?$)\\d{1,13}(\\.\\d{1,2})?$", message = "amount must be > 0 with up to 2 decimals")
    private String amount;

    @NotBlank
    @Size(min = 3, max = 3)
    private String currency;

    @NotBlank
    @Pattern(regexp = "^\\d{12,19}$", message = "cardNumber must be 12-19 digits")
    @Luhn
    private String cardNumber;

    @NotBlank
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "expMonth must be 01-12")
    private String expMonth;

    @NotBlank
    @Pattern(regexp = "^20\\d{2}$", message = "expYear must be 4-digit year >= 2000")
    private String expYear;

    @NotBlank
    @Pattern(regexp = "^\\d{3,4}$", message = "cvv must be 3 or 4 digits")
    private String cvv;

    @Size(max = 64)
    private String invoiceNumber;

    @Size(max = 96)
    private String cardHolderName;

    @Size(max = 128)
    private String address;

    @Size(max = 16)
    private String postalCode;

    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getExpMonth() { return expMonth; }
    public void setExpMonth(String expMonth) { this.expMonth = expMonth; }
    public String getExpYear() { return expYear; }
    public void setExpYear(String expYear) { this.expYear = expYear; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
}


