package com.example.converge.dto.response;

public class SaleResponse {
    private boolean approved;
    private String authCode;
    private String transactionId;
    private String avsResult;
    private String cvvResult;
    private String message;
    private String rawCode;
    private String rawText;
    private String timestamp;

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getAuthCode() { return authCode; }
    public void setAuthCode(String authCode) { this.authCode = authCode; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getAvsResult() { return avsResult; }
    public void setAvsResult(String avsResult) { this.avsResult = avsResult; }
    public String getCvvResult() { return cvvResult; }
    public void setCvvResult(String cvvResult) { this.cvvResult = cvvResult; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRawCode() { return rawCode; }
    public void setRawCode(String rawCode) { this.rawCode = rawCode; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}


