package com.argocd.dashboard.view;

import com.argocd.dashboard.model.Application;

import java.util.List;

public interface ApplicationView {
    void displayApplications(List<Application> applications);
}
