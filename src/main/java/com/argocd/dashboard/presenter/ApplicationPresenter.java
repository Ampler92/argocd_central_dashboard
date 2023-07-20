package com.argocd.dashboard.presenter;

import com.argocd.dashboard.config.RestClientConfig;
import com.argocd.dashboard.model.*;
import com.argocd.dashboard.view.ApplicationView;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class ApplicationPresenter {

    private final RestClientConfig restClientConfig;
    private final ApplicationView applicationView;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ApplicationPresenter(RestClientConfig restClientConfig, ApplicationView applicationView, RestTemplate restTemplate) {
        this.restClientConfig = restClientConfig;
        this.applicationView = applicationView;
        this.restTemplate = restTemplate;
    }

    @Cacheable("applications")
    public List<Application> loadApplications() {
        List<Application> applications = new ArrayList<>();

        for (ArgoCDInstance instance : restClientConfig.getInstances()) {
            if (instance.isDisconnected()) {
                log.warn("Skipping disconnected instance '{}'.", instance.getName());
                continue;
            }

            String jwtToken = getJwtTokenForInstance(instance);
            if (jwtToken != null) {
                try {
                    List<Application> instanceApplications = fetchApplicationsForInstance(instance, jwtToken);
                    instanceApplications.forEach(app -> app.getMetadata().setInstance(instance.getName()));
                    applications.addAll(instanceApplications);
                } catch (Exception e) {
                    log.error("Error loading applications for instance '{}'. Skipping...", instance.getName());
                    e.printStackTrace();
                }
            } else {
                log.error("Unable to retrieve JWT token for instance '{}'. Skipping...", instance.getName());
            }
        }

        applicationView.displayApplications(applications);
        return applications;
    }


    public List<Resource> getApplicationResources(String appName, String instanceName) {
        List<Application> applications = loadApplications();

        // Find the application with the specified appName and instanceName in the cached list
        Optional<Application> optionalApplication = applications.stream()
                .filter(application ->
                        application.getMetadata().getName().equals(appName)
                                && application.getMetadata().getInstance().equals(instanceName))
                .findFirst();

        if (optionalApplication.isPresent()) {
            Application application = optionalApplication.get();
            log.info("Application '{}' found in instance '{}' resources '{}'.", appName, instanceName, Arrays.toString(application.getStatus().getResources().toArray()));
            return new ArrayList<>(application.getStatus().getResources());
        } else {
            log.error("Application '{}' not found in instance '{}'.", appName, instanceName);
            return Collections.emptyList();
        }
    }

    public ResponseEntity<Object> syncApplication(String instanceName, String appName) {
        ArgoCDInstance instance = restClientConfig.getInstanceByName(instanceName);
        if (instance == null) {
            log.error("Instance '{}' not found.", instanceName);
            return ResponseEntity.badRequest().body("Instance not found " + instanceName);
        }

        String jwtToken = getJwtTokenForInstance(instance);
        if (jwtToken == null) {
            log.error("Unable to retrieve JWT token for instance '{}'.", instanceName);
            return ResponseEntity.badRequest().body("Unable to retrieve JWT token for instance " + instanceName);
        }

        Application application = findApplicationByName(instance, jwtToken, appName);
        if (application == null) {
            log.error("Application '{}' not found in instance '{}'.", appName, instanceName);
            return ResponseEntity.badRequest().body("Application not found " + appName);
        }

        return new ResponseEntity<>(Objects.requireNonNull(triggerApplicationSync(instance, jwtToken, application)).getBody(),HttpStatus.OK) ;
    }

    private String getJwtTokenForInstance(ArgoCDInstance instance) {
        return restClientConfig.getJwtTokenForInstance(instance, restTemplate);
    }

    private List<Application> fetchApplicationsForInstance(ArgoCDInstance instance, String jwtToken) {
        List<Application> applications = new ArrayList<>();
        String authEndpointUrl = instance.getUrl() + "/api/v1/applications";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<ApplicationResponseWrapper> response = restTemplate.exchange(
                    authEndpointUrl, HttpMethod.GET, entity, ApplicationResponseWrapper.class);

            ApplicationResponseWrapper responseWrapper = response.getBody();
            if (responseWrapper != null && responseWrapper.getItems() != null) {
                applications.addAll(Arrays.asList(responseWrapper.getItems()));
            }
        } catch (RestClientException e) {
            // RestClientException occurred
            e.printStackTrace();
        } catch (Exception e) {
            // Other exceptions occurred
            e.printStackTrace();
        }

        return applications;
    }

    private Application findApplicationByName(ArgoCDInstance instance, String jwtToken, String appName) {
        List<Application> applications = fetchApplicationsForInstance(instance, jwtToken);
        return applications.stream()
                .filter(app -> appName.equals(app.getMetadata().getName()))
                .findFirst()
                .orElse(null);
    }

    public Application findApplicationByName(String instanceName, String appName) {
        ArgoCDInstance instance = restClientConfig.getInstanceByName(instanceName);
        String jwtToken = getJwtTokenForInstance(instance);
        List<Application> applications = fetchApplicationsForInstance(instance, jwtToken);
        return applications.stream()
                .filter(app -> appName.equals(app.getMetadata().getName()))
                .findFirst()
                .orElse(null);
    }

    private ResponseEntity<ApplicationSyncResponse> triggerApplicationSync(ArgoCDInstance instance, String jwtToken, Application application) {
        String authEndpointUrl = instance.getUrl() + "/api/v1/applications/" + application.getMetadata().getName() + "/sync";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            return restTemplate.exchange(
                    authEndpointUrl, HttpMethod.POST, entity, ApplicationSyncResponse.class);

        } catch (RestClientException e) {
            // RestClientException occurred
            e.printStackTrace();
        } catch (Exception e) {
            // Other exceptions occurred
            e.printStackTrace();
        }
        return null;
    }
}
