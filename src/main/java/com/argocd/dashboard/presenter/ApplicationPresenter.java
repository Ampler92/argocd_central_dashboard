package com.argocd.dashboard.presenter;

import com.argocd.dashboard.config.RestClientConfig;
import com.argocd.dashboard.model.Application;
import com.argocd.dashboard.model.ApplicationResponseWrapper;
import com.argocd.dashboard.model.ArgoCDInstance;
import com.argocd.dashboard.model.Resource;
import com.argocd.dashboard.view.ApplicationView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
            String jwtToken = getJwtTokenForInstance(instance);
            if (jwtToken != null) {
                List<Application> instanceApplications = fetchApplicationsForInstance(instance, jwtToken);
                instanceApplications.forEach(app -> app.getMetadata().setInstance(instance.getName()));
                applications.addAll(instanceApplications);
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

    public void syncApplication(String instanceName, String appName) {
        ArgoCDInstance instance = restClientConfig.getInstanceByName(instanceName);
        if (instance == null) {
            log.error("Instance '{}' not found.", instanceName);
            return;
        }

        String jwtToken = getJwtTokenForInstance(instance);
        if (jwtToken == null) {
            log.error("Unable to retrieve JWT token for instance '{}'.", instanceName);
            return;
        }

        Application application = findApplicationByName(instance, jwtToken, appName);
        if (application == null) {
            log.error("Application '{}' not found in instance '{}'.", appName, instanceName);
            return;
        }

        boolean isSyncSuccessful = triggerApplicationSync(instance, jwtToken, application);
        if (isSyncSuccessful) {
            log.info("Sync for application '{}' on instance '{}' was successful.", appName, instanceName);
        } else {
            log.error("Sync for application '{}' on instance '{}' failed.", appName, instanceName);
        }
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

    private boolean triggerApplicationSync(ArgoCDInstance instance, String jwtToken, Application application) {
        String authEndpointUrl = instance.getUrl() + "/api/v1/applications/" + application.getMetadata().getName() + "/sync";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + jwtToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    authEndpointUrl, HttpMethod.POST, entity, String.class);

            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            // RestClientException occurred
            e.printStackTrace();
        } catch (Exception e) {
            // Other exceptions occurred
            e.printStackTrace();
        }

        return false;
    }
}
