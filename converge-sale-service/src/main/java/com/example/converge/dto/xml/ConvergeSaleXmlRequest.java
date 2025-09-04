package com.example.converge.dto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "txn")
@XmlAccessorType(XmlAccessType.FIELD)
public class ConvergeSaleXmlRequest {

    @XmlElement(name = "ssl_transaction_type")
    private String transactionType = "ccsale";
    
    @XmlElement(name = "ssl_merchant_id")
    private String merchantId;
    @XmlElement(name = "ssl_user_id")
    private String userId;
    @XmlElement(name = "ssl_pin")
    private String pin;
    @XmlElement(name = "ssl_vendor_id")
    private String vendorId;

    @XmlElement(name = "ssl_amount")
    private String amount;
    @XmlElement(name = "ssl_card_number")
    private String cardNumber;
    @XmlElement(name = "ssl_exp_date")
    private String expDateMmYy;
    @XmlElement(name = "ssl_cvv2cvc2_indicator")
    private String cvvIndicator = "1";
    @XmlElement(name = "ssl_cvv2cvc2")
    private String cvv;

    @XmlElement(name = "ssl_avs_zip")
    private String avsZip;
    @XmlElement(name = "ssl_avs_address")
    private String avsAddress;
    @XmlElement(name = "ssl_get_token")
    private String getToken = "Y";
    @XmlElement(name = "ssl_add_token")
    private String addToken = "Y";
    @XmlElement(name = "ssl_invoice_number")
    private String invoiceNumber;
    @XmlElement(name = "ssl_first_name")
    private String firstName;
    @XmlElement(name = "ssl_last_name")
    private String lastName;
    @XmlElement(name = "ssl_address2")
    private String address2;
    @XmlElement(name = "ssl_city")
    private String city;
    @XmlElement(name = "ssl_state")
    private String state;
    @XmlElement(name = "ssl_country")
    private String country = "USA";
    @XmlElement(name = "ssl_email")
    private String email;
    @XmlElement(name = "ssl_phone")
    private String phone;

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getVendorId() { return vendorId; }
    public void setVendorId(String vendorId) { this.vendorId = vendorId; }
    public String getAmount() { return amount; }
    public void setAmount(String amount) { this.amount = amount; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getExpDateMmYy() { return expDateMmYy; }
    public void setExpDateMmYy(String expDateMmYy) { this.expDateMmYy = expDateMmYy; }
    public String getCvvIndicator() { return cvvIndicator; }
    public void setCvvIndicator(String cvvIndicator) { this.cvvIndicator = cvvIndicator; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
    public String getAvsZip() { return avsZip; }
    public void setAvsZip(String avsZip) { this.avsZip = avsZip; }
    public String getAvsAddress() { return avsAddress; }
    public void setAvsAddress(String avsAddress) { this.avsAddress = avsAddress; }
    public String getGetToken() { return getToken; }
    public void setGetToken(String getToken) { this.getToken = getToken; }
    public String getAddToken() { return addToken; }
    public void setAddToken(String addToken) { this.addToken = addToken; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}


