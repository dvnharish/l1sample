package com.example.converge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "converge")
public class ConvergeProperties {
    private String baseUrl;
    private String sslMerchantId;
    private String sslUserId;
    private String sslPin;
    private int timeoutMs = 10000;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getSslMerchantId() { return sslMerchantId; }
    public void setSslMerchantId(String sslMerchantId) { this.sslMerchantId = sslMerchantId; }
    public String getSslUserId() { return sslUserId; }
    public void setSslUserId(String sslUserId) { this.sslUserId = sslUserId; }
    public String getSslPin() { return sslPin; }
    public void setSslPin(String sslPin) { this.sslPin = sslPin; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}


