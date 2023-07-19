package com.argocd.dashboard.model;

import lombok.Data;

@Data
public class ApplicationSpec {
    private Source source;
    private Destination destination;
    private String project;
    private SyncPolicy syncPolicy;

    // Getters and setters
}
