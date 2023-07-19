package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class Application {
    private Metadata metadata;
    private ApplicationSpec spec;
    private ApplicationStatus status;
}

