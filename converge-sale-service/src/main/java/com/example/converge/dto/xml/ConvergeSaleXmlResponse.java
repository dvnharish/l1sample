package com.example.converge.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "txn")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConvergeSaleXmlResponse {

    @XmlElement(name = "ssl_card_number")
    private String cardNumber;
    @XmlElement(name = "ssl_exp_date")
    private String expDate;
    @XmlElement(name = "ssl_amount")
    private String amount;
    @XmlElement(name = "ssl_result")
    private String result;
    @XmlElement(name = "ssl_result_message")
    private String resultMessage;
    @XmlElement(name = "ssl_txn_id")
    private String transactionId;
    @XmlElement(name = "ssl_approval_code")
    private String approvalCode;
    @XmlElement(name = "ssl_account_balance")
    private String accountBalance;
    @XmlElement(name = "ssl_txn_time")
    private String txnTime;

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getExpDate() { return expDate; }
    public void setExpDate(String expDate) { this.expDate = expDate; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getApprovalCode() { return approvalCode; }
    public void setApprovalCode(String approvalCode) { this.approvalCode = approvalCode; }
    public String getAccountBalance() { return accountBalance; }
    public void setAccountBalance(String accountBalance) { this.accountBalance = accountBalance; }
    public String getTxnTime() { return txnTime; }
    public void setTxnTime(String txnTime) { this.txnTime = txnTime; }
}


