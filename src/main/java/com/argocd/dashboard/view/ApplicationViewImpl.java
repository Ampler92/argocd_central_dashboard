package com.argocd.dashboard.view;

import com.argocd.dashboard.model.Application;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class ApplicationViewImpl implements ApplicationView {

    @Override
    public void displayApplications(List<Application> applications) {
        // Implement your code here to display applications, if needed.
        // For this example, we'll print the applications to the console.
        applications.forEach(application -> log.info(application.getMetadata().getName()));
    }
}
