package com.example.converge.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "txn")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConvergeSaleXmlResponse {

    @XmlElement(name = "ssl_issuer_response")
    private String issuerResponse;
    @XmlElement(name = "ssl_card_number")
    private String cardNumber;
    @XmlElement(name = "ssl_departure_date")
    private String departureDate;
    @XmlElement(name = "ssl_merchant_txn_id")
    private String merchantTxnId;
    @XmlElement(name = "ssl_oar_data")
    private String oarData;
    @XmlElement(name = "ssl_result")
    private String result;
    @XmlElement(name = "ssl_txn_id")
    private String transactionId;
    @XmlElement(name = "ssl_avs_response")
    private String avsResponse;
    @XmlElement(name = "ssl_approval_code")
    private String approvalCode;
    @XmlElement(name = "ssl_amount")
    private String amount;
    @XmlElement(name = "ssl_txn_time")
    private String txnTime;
    @XmlElement(name = "ssl_description")
    private String description;
    @XmlElement(name = "ssl_exp_date")
    private String expDate;
    @XmlElement(name = "ssl_card_short_description")
    private String cardShortDescription;
    @XmlElement(name = "ssl_completion_date")
    private String completionDate;
    @XmlElement(name = "ssl_get_token")
    private String getToken;
    @XmlElement(name = "ssl_customer_code")
    private String customerCode;
    @XmlElement(name = "ssl_card_type")
    private String cardType;
    @XmlElement(name = "ssl_transaction_type")
    private String transactionType;
    @XmlElement(name = "ssl_salestax")
    private String salesTax;
    @XmlElement(name = "ssl_account_balance")
    private String accountBalance;
    @XmlElement(name = "ssl_ps2000_data")
    private String ps2000Data;
    @XmlElement(name = "ssl_result_message")
    private String resultMessage;
    @XmlElement(name = "ssl_invoice_number")
    private String invoiceNumber;
    @XmlElement(name = "ssl_cvv2_response")
    private String cvv2Response;
    @XmlElement(name = "ssl_partner_app_id")
    private String partnerAppId;
    
    // Error response fields
    @XmlElement(name = "errorCode")
    private String errorCode;
    @XmlElement(name = "errorName")
    private String errorName;
    @XmlElement(name = "errorMessage")
    private String errorMessage;

    public String getIssuerResponse() { return issuerResponse; }
    public void setIssuerResponse(String issuerResponse) { this.issuerResponse = issuerResponse; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getDepartureDate() { return departureDate; }
    public void setDepartureDate(String departureDate) { this.departureDate = departureDate; }
    public String getMerchantTxnId() { return merchantTxnId; }
    public void setMerchantTxnId(String merchantTxnId) { this.merchantTxnId = merchantTxnId; }
    public String getOarData() { return oarData; }
    public void setOarData(String oarData) { this.oarData = oarData; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAvsResponse() { return avsResponse; }
    public void setAvsResponse(String avsResponse) { this.avsResponse = avsResponse; }
    public String getApprovalCode() { return approvalCode; }
    public void setApprovalCode(String approvalCode) { this.approvalCode = approvalCode; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getTxnTime() { return txnTime; }
    public void setTxnTime(String txnTime) { this.txnTime = txnTime; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getExpDate() { return expDate; }
    public void setExpDate(String expDate) { this.expDate = expDate; }
    public String getCardShortDescription() { return cardShortDescription; }
    public void setCardShortDescription(String cardShortDescription) { this.cardShortDescription = cardShortDescription; }
    public String getCompletionDate() { return completionDate; }
    public void setCompletionDate(String completionDate) { this.completionDate = completionDate; }
    public String getGetToken() { return getToken; }
    public void setGetToken(String getToken) { this.getToken = getToken; }
    public String getCustomerCode() { return customerCode; }
    public void setCustomerCode(String customerCode) { this.customerCode = customerCode; }
    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getSalesTax() { return salesTax; }
    public void setSalesTax(String salesTax) { this.salesTax = salesTax; }
    public String getAccountBalance() { return accountBalance; }
    public void setAccountBalance(String accountBalance) { this.accountBalance = accountBalance; }
    public String getPs2000Data() { return ps2000Data; }
    public void setPs2000Data(String ps2000Data) { this.ps2000Data = ps2000Data; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getCvv2Response() { return cvv2Response; }
    public void setCvv2Response(String cvv2Response) { this.cvv2Response = cvv2Response; }
    public String getPartnerAppId() { return partnerAppId; }
    public void setPartnerAppId(String partnerAppId) { this.partnerAppId = partnerAppId; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorName() { return errorName; }
    public void setErrorName(String errorName) { this.errorName = errorName; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}


