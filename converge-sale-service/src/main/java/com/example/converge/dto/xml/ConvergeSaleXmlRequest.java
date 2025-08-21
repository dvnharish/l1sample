package com.example.converge.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "txn")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConvergeSaleXmlRequest {

    @XmlElement(name = "ssl_merchant_id")
    private String merchantId;
    @XmlElement(name = "ssl_user_id")
    private String userId;
    @XmlElement(name = "ssl_pin")
    private String pin;

    @XmlElement(name = "ssl_transaction_type")
    private String transactionType = "ccsale";

    @XmlElement(name = "ssl_amount")
    private String amount;
    @XmlElement(name = "ssl_card_number")
    private String cardNumber;
    @XmlElement(name = "ssl_exp_date")
    private String expDateMmYy;
    @XmlElement(name = "ssl_cvv2cvc2")
    private String cvv;

    @XmlElement(name = "ssl_invoice_number")
    private String invoiceNumber;
    @XmlElement(name = "ssl_avs_address")
    private String avsAddress;
    @XmlElement(name = "ssl_avs_zip")
    private String avsZip;
    @XmlElement(name = "ssl_cardholder_name")
    private String cardholderName;
    @XmlElement(name = "ssl_transaction_currency")
    private String transactionCurrency;

    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getExpDateMmYy() { return expDateMmYy; }
    public void setExpDateMmYy(String expDateMmYy) { this.expDateMmYy = expDateMmYy; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getAvsAddress() { return avsAddress; }
    public void setAvsAddress(String avsAddress) { this.avsAddress = avsAddress; }
    public String getAvsZip() { return avsZip; }
    public void setAvsZip(String avsZip) { this.avsZip = avsZip; }
    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
    public String getTransactionCurrency() { return transactionCurrency; }
    public void setTransactionCurrency(String transactionCurrency) { this.transactionCurrency = transactionCurrency; }
}


