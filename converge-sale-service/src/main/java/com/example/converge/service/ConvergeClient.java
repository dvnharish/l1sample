package com.example.converge.service;

import com.example.converge.config.ConvergeProperties;
import com.example.converge.dto.xml.ConvergeSaleXmlRequest;
import com.example.converge.dto.xml.ConvergeSaleXmlResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.io.StringWriter;

@Component
public class ConvergeClient {

    private final RestTemplate restTemplate;
    private final ConvergeProperties properties;

    public ConvergeClient(RestTemplate restTemplate, ConvergeProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 300), include = {RestClientException.class})
    public ConvergeSaleXmlResponse sale(ConvergeSaleXmlRequest xmlReq) {
        String xml = marshal(xmlReq);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("xmldata", xml);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(properties.getBaseUrl(), entity, String.class);
        return unmarshal(resp.getBody());
    }

    private String marshal(ConvergeSaleXmlRequest request) {
        try {
            JAXBContext context = JAXBContext.newInstance(ConvergeSaleXmlRequest.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            StringWriter writer = new StringWriter();
            writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            marshaller.marshal(request, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to marshal XML request", e);
        }
    }

    private ConvergeSaleXmlResponse unmarshal(String xml) {
        try {
            JAXBContext context = JAXBContext.newInstance(ConvergeSaleXmlResponse.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            return (ConvergeSaleXmlResponse) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to unmarshal XML response", e);
        }
    }
}


