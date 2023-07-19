package com.argocd.dashboard.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class AuthorizationHeaderInterceptor implements ClientHttpRequestInterceptor {

    private final String jwtToken;

    public AuthorizationHeaderInterceptor(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        headers.add("Authorization", "Bearer " + jwtToken);
        return execution.execute(request, body);
    }
}
