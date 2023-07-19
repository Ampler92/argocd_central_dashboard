package com.argocd.dashboard.controller;

import com.argocd.dashboard.model.Application;
import com.argocd.dashboard.model.Resource;
import com.argocd.dashboard.presenter.ApplicationPresenter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    private final ApplicationPresenter applicationPresenter;

    public ApplicationController(ApplicationPresenter applicationPresenter) {
        this.applicationPresenter = applicationPresenter;
    }

    @GetMapping
    public List<Application> getApplications() {
        return applicationPresenter.loadApplications();
    }
    @PostMapping("/sync/{instance}/{appName}")
    public void syncApplication(@PathVariable String appName, @PathVariable String instance) {
        applicationPresenter.syncApplication(instance,appName);
    }
    @GetMapping("/{instance}/{appName}/resources")
    public ResponseEntity<List<Resource>> getApplicationResources(@PathVariable String instance, @PathVariable String appName) {
        // Get the resources for the application
        List<Resource> resources = applicationPresenter.getApplicationResources(appName, instance);

        // Return the resources in the response body with HTTP status 200 (OK)
        return ResponseEntity.ok(resources);
    }
}
