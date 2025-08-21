package com.example.converge.service;

import com.example.converge.config.ConvergeProperties;
import com.example.converge.dto.request.SaleRequest;
import com.example.converge.dto.response.SaleResponse;
import com.example.converge.dto.xml.ConvergeSaleXmlRequest;
import com.example.converge.dto.xml.ConvergeSaleXmlResponse;
import com.example.converge.mapper.ConvergeMapper;
import org.springframework.stereotype.Service;

@Service
public class SaleService {

    private final ConvergeClient client;
    private final ConvergeProperties properties;

    public SaleService(ConvergeClient client, ConvergeProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    public SaleResponse processSale(SaleRequest request) {
        ConvergeSaleXmlRequest xmlRequest = ConvergeMapper.toXmlRequest(request, properties);
        ConvergeSaleXmlResponse xmlResponse = client.sale(xmlRequest);
        return ConvergeMapper.toSaleResponse(xmlResponse);
    }
}


