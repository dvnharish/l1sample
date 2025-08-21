package com.example.converge.mapper;

import com.example.converge.config.ConvergeProperties;
import com.example.converge.dto.request.SaleRequest;
import com.example.converge.dto.response.SaleResponse;
import com.example.converge.dto.xml.ConvergeSaleXmlRequest;
import com.example.converge.dto.xml.ConvergeSaleXmlResponse;

public class ConvergeMapper {

    public static ConvergeSaleXmlRequest toXmlRequest(SaleRequest req, ConvergeProperties props) {
        ConvergeSaleXmlRequest xml = new ConvergeSaleXmlRequest();
        xml.setMerchantId(props.getSslMerchantId());
        xml.setUserId(props.getSslUserId());
        xml.setPin(props.getSslPin());
        xml.setAmount(req.getAmount());
        xml.setCardNumber(req.getCardNumber());
        xml.setExpDateMmYy(buildExpDate(req.getExpMonth(), req.getExpYear()));
        xml.setCvv(req.getCvv());
        xml.setInvoiceNumber(req.getInvoiceNumber());
        xml.setAvsAddress(req.getAddress());
        xml.setAvsZip(req.getPostalCode());
        xml.setCardholderName(req.getCardHolderName());
        xml.setTransactionCurrency(req.getCurrency());
        return xml;
    }

    public static SaleResponse toSaleResponse(ConvergeSaleXmlResponse xml) {
        SaleResponse res = new SaleResponse();
        boolean approved = "0".equalsIgnoreCase(xml.getResult());
        res.setApproved(approved);
        res.setAuthCode(xml.getApprovalCode());
        res.setTransactionId(xml.getTransactionId());
        res.setAvsResult(xml.getAvsResponse());
        res.setCvvResult(xml.getCvv2Response());
        res.setMessage(xml.getResultMessage());
        res.setRawCode(xml.getIssuerResponse());
        res.setRawText(xml.getResultMessage());
        res.setTimestamp(xml.getTxnTime());
        return res;
    }

    private static String buildExpDate(String month, String year) {
        String yy = year.length() >= 2 ? year.substring(year.length() - 2) : year;
        return month + yy;
    }
}


