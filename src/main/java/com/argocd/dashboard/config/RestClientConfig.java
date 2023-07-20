package com.argocd.dashboard.config;

import com.argocd.dashboard.model.ArgoCDInstance;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

@Configuration
@Slf4j
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "argocd-instances")
public class RestClientConfig {

    private List<ArgoCDInstance> instances;

    public List<ArgoCDInstance> getInstances() {
        return instances;
    }

    public void setInstances(List<ArgoCDInstance> instances) {
        this.instances = instances;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(Objects.requireNonNull(createHttpRequestFactoryWithSslVerificationDisabled()));
        configureInstances(restTemplate);
        return restTemplate;
    }

    private void configureInstances(RestTemplate restTemplate) {
        for (ArgoCDInstance instance : instances) {
            try {
                String jwtToken = getJwtTokenForInstance(instance, restTemplate);
                if (jwtToken != null) {
                    restTemplate.getInterceptors().add(new AuthorizationHeaderInterceptor(jwtToken));
                } else {
                    instance.setDisconnected(true);
                    log.error("Unable to init an instance '{}'.", instance.getName());
                }
            } catch (Exception e) {
                instance.setDisconnected(true);
                log.error("Error configuring instance '{}'.", instance.getName());
                e.printStackTrace();
            }
        }
    }


    private HttpComponentsClientHttpRequestFactory createHttpRequestFactoryWithSslVerificationDisabled() {
        TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
            HttpClient httpClient = HttpClients.custom()
                    .setSslcontext(sslContext)
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            return new HttpComponentsClientHttpRequestFactory(httpClient);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getJwtTokenForInstance(ArgoCDInstance instance, RestTemplate restTemplate) {
        String authEndpointUrl = instance.getUrl() + "/api/v1/session";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Replace 'username' and 'password' with the actual property names used by the Argo CD API
        JSONObject requestBody = new JSONObject();
        requestBody.put("username", instance.getUsername());
        requestBody.put("password", instance.getPassword());

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(authEndpointUrl, HttpMethod.POST, requestEntity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            // Replace 'token' with the actual property name used by the Argo CD API to provide the JWT token
            JSONObject responseBody = new JSONObject(responseEntity.getBody());
            return responseBody.getString("token");
        }

        // If the authentication is not successful, return null
        return null;
    }
    public ArgoCDInstance getInstanceByName(String instanceName) {
        return instances.stream()
                .filter(instance -> instanceName.equals(instance.getName()))
                .findFirst()
                .orElse(null);
    }
}
