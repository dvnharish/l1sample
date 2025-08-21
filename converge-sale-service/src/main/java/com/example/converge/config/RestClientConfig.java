package com.example.converge.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(ConvergeProperties.class)
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, ConvergeProperties properties) {
        RestTemplate restTemplate = builder
            .setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
            .additionalMessageConverters(jaxbConverter())
            .build();

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws java.io.IOException {
                int raw = response.getRawStatusCode();
                return raw >= 500;
            }
        });

        List<HttpMessageConverter<?>> converters = restTemplate.getMessageConverters();
        converters.add(0, jaxbConverter());
        return restTemplate;
    }

    @Bean
    public Jaxb2RootElementHttpMessageConverter jaxbConverter() {
        return new Jaxb2RootElementHttpMessageConverter();
    }
}


