package com.example.converge.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "txn")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConvergeSaleXmlResponse {

    @XmlElement(name = "ssl_result")
    private String result;
    @XmlElement(name = "ssl_result_message")
    private String resultMessage;
    @XmlElement(name = "ssl_txn_id")
    private String transactionId;
    @XmlElement(name = "ssl_approval_code")
    private String approvalCode;
    @XmlElement(name = "ssl_avs_response")
    private String avsResponse;
    @XmlElement(name = "ssl_cvv2_response")
    private String cvv2Response;
    @XmlElement(name = "ssl_txn_time")
    private String txnTime;
    @XmlElement(name = "ssl_issuer_response")
    private String issuerResponse;

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getApprovalCode() { return approvalCode; }
    public void setApprovalCode(String approvalCode) { this.approvalCode = approvalCode; }
    public String getAvsResponse() { return avsResponse; }
    public void setAvsResponse(String avsResponse) { this.avsResponse = avsResponse; }
    public String getCvv2Response() { return cvv2Response; }
    public void setCvv2Response(String cvv2Response) { this.cvv2Response = cvv2Response; }
    public String getTxnTime() { return txnTime; }
    public void setTxnTime(String txnTime) { this.txnTime = txnTime; }
    public String getIssuerResponse() { return issuerResponse; }
    public void setIssuerResponse(String issuerResponse) { this.issuerResponse = issuerResponse; }
}


