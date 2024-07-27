package com.crypto.trading.trading.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class ApiService {

    @Autowired
    private RestTemplate restTemplate;

    public  ResponseEntity<String> callApi(String url, HttpHeaders headers) {
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return  restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

}