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
        xml.setVendorId(props.getSslVendorId());
        xml.setAmount(req.getAmount());
        xml.setCardNumber(req.getCardNumber());
        xml.setExpDateMmYy(buildExpDate(req.getExpMonth(), req.getExpYear()));
        xml.setCvv(req.getCvv());
        xml.setInvoiceNumber(req.getInvoiceNumber());
        xml.setAvsAddress(req.getAddress());
        xml.setAvsZip(req.getPostalCode());
        
        // Parse cardholder name into first and last name
        if (req.getCardHolderName() != null && !req.getCardHolderName().trim().isEmpty()) {
            String[] nameParts = req.getCardHolderName().trim().split("\\s+", 2);
            xml.setFirstName(nameParts[0]);
            if (nameParts.length > 1) {
                xml.setLastName(nameParts[1]);
            }
        }
        
        return xml;
    }

    public static SaleResponse toSaleResponse(ConvergeSaleXmlResponse xml) {
        SaleResponse res = new SaleResponse();
        
        // Map approval status - "0" means approved in Converge
        boolean approved = "0".equalsIgnoreCase(xml.getResult());
        res.setApproved(approved);
        
        // Map transaction details
        res.setAuthCode(xml.getApprovalCode());
        res.setTransactionId(xml.getTransactionId());
        
        // Map verification results - these fields are not in the actual response
        res.setAvsResult(null);
        res.setCvvResult(null);
        
        // Map response messages
        res.setMessage(xml.getResultMessage());
        res.setRawCode(xml.getResult()); // The result code
        res.setRawText(xml.getResultMessage()); // The result message as raw text
        
        // Map timestamp
        res.setTimestamp(xml.getTxnTime());
        
        return res;
    }

    private static String buildExpDate(String month, String year) {
        String yy = year.length() >= 2 ? year.substring(year.length() - 2) : year;
        return month + yy;
    }
}


